/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.oveduumnakal.betterblackjacking;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import net.runelite.api.ChatMessageType;
import net.runelite.api.gameval.AnimationID;

/**
 * Tracks which blackjack targets are knocked out or attacking the player, and how many guaranteed
 * pickpockets are left (PLAN §4).
 *
 * <p>It holds no RuneLite {@code Client}, NPC or event objects: NPCs are identified by their index
 * and every method takes plain values, so the plugin translates events into these calls and tests
 * can replay recorded sessions. Every event method takes the game tick it happened on, and a newer
 * tick than the tracker has seen advances it as {@link #onTick(int)} would, so events and the tick
 * can arrive in either order within a tick.
 *
 * <p>One target is knocked out at a time: {@link TargetState#SAFE} while another pickpocket will
 * land, then {@link TargetState#WAKING}, then {@link TargetState#KNOCK_OUT} on the wake tick or
 * earlier if the game says it woke. Any number of targets can be {@link TargetState#ATTACKING}.
 * Every other NPC is {@link TargetState#KNOCK_OUT}.
 */
public final class KnockoutTracker
{
	/** How many ticks a knocked-out target stays down by default: knocked out at T, awake at T+5. */
	public static final int DEFAULT_KNOCK_OUT_TICKS = 5;
	/** A pickpocket lands this many ticks after the game processes it. */
	public static final int PICKPOCKET_LANDING_DELAY = 1;
	/** Consecutive pickpockets are processed at least this many ticks apart. */
	public static final int PICKPOCKET_INTERVAL = 2;
	/** At most this many guaranteed pickpockets fit into one knock-out. */
	public static final int MAX_PICKPOCKETS = 2;
	/** An attacking target goes back to knock-out after this many ticks without interacting with the player. */
	public static final int ATTACKING_EXIT_TICKS = 2;
	/** The NPC index meaning "no NPC". */
	public static final int NO_NPC = -1;
	/** The tick meaning "no knock-out". */
	public static final int NO_TICK = -1;

	private static final int UNSET = Integer.MIN_VALUE;

	private final Set<Integer> interactingWithPlayer = new HashSet<>();
	private final Map<Integer, Integer> attackingSeenTick = new HashMap<>();
	private int knockOutTicks = DEFAULT_KNOCK_OUT_TICKS;
	private int currentTick = UNSET;
	private PickpocketBudget budget;
	private int knockedOutNpc = NO_NPC;

	/**
	 * Sets how many ticks a knocked-out target stays down. It applies from the next knock-out.
	 *
	 * @param ticks the {@code knockOutTicks} config value; values below 1 count as 1
	 */
	public void setKnockOutTicks(int ticks)
	{
		knockOutTicks = Math.max(1, ticks);
	}

	/**
	 * How many ticks the next knock-out will last.
	 *
	 * @return the duration in ticks
	 */
	public int knockOutTicks()
	{
		return knockOutTicks;
	}

	/**
	 * Advances to a game tick: ends a knock-out whose wake tick has come, and returns attacking
	 * targets to knock-out once they haven't interacted with the player for
	 * {@link #ATTACKING_EXIT_TICKS} ticks. Calling it again with the same tick does nothing.
	 *
	 * @param tick the current game tick
	 */
	public void onTick(int tick)
	{
		if (tick == currentTick)
			return;

		currentTick = tick;
		if (budget != null && tick >= budget.wakeTick())
			clearKnockOut();

		Iterator<Map.Entry<Integer, Integer>> it = attackingSeenTick.entrySet().iterator();
		while (it.hasNext())
		{
			Map.Entry<Integer, Integer> entry = it.next();
			if (interactingWithPlayer.contains(entry.getKey()))
				entry.setValue(tick);
			else if (tick - entry.getValue() >= ATTACKING_EXIT_TICKS)
				it.remove();
		}
	}

	/**
	 * Handles a chat message. Only game messages ({@code GAMEMESSAGE} and {@code SPAM}) count.
	 *
	 * <ul>
	 * <li>A knock-out success starts a knock-out on {@code interactingNpcIndex}, which becomes
	 * {@link TargetState#SAFE}. If that is {@link #NO_NPC}, the {@code Zzzzzz} overhead names it.</li>
	 * <li>A failed knock-out or either "during combat" message makes {@code interactingNpcIndex}
	 * {@link TargetState#ATTACKING}.</li>
	 * <li>A processed pickpocket moves the earliest next pickpocket two ticks on.</li>
	 * </ul>
	 *
	 * @param tick the tick the message arrived on
	 * @param type the message's chat type
	 * @param message the raw message text
	 * @param interactingNpcIndex the index of the NPC the player is interacting with, or {@link #NO_NPC}
	 */
	public void onChat(int tick, ChatMessageType type, String message, int interactingNpcIndex)
	{
		if (type != ChatMessageType.GAMEMESSAGE && type != ChatMessageType.SPAM)
			return;

		ChatSignals.Message signal = ChatSignals.classifyMessage(message);
		if (signal == null)
			return;

		onTick(tick);
		switch (signal)
		{
			case KNOCKOUT_SUCCESS:
				budget = new PickpocketBudget(tick, knockOutTicks);
				knockedOutNpc = NO_NPC;
				setKnockedOutNpc(interactingNpcIndex);
				break;
			case KNOCKOUT_FAILED:
			case KNOCKOUT_IN_COMBAT:
			case PICKPOCKET_IN_COMBAT:
				markAttacking(interactingNpcIndex, tick);
				break;
			case PICKPOCKET_PROCESSED:
				if (budget != null)
					budget.onPickpocketProcessed(tick);

				break;
			default:
				break;
		}
	}

	/**
	 * Handles an NPC's overhead text.
	 *
	 * <ul>
	 * <li>{@code Zzzzzz} on the knock-out tick names the knocked-out NPC, overriding the interacting
	 * NPC the success message was credited to.</li>
	 * <li>{@code Arghh my head.} from the knocked-out NPC ends the knock-out.</li>
	 * <li>{@code I'll kill you for that!} makes the NPC {@link TargetState#ATTACKING}.</li>
	 * </ul>
	 *
	 * @param tick the tick the text appeared on
	 * @param npcIndex the index of the NPC that said it
	 * @param text the raw overhead text
	 */
	public void onOverhead(int tick, int npcIndex, String text)
	{
		ChatSignals.Overhead signal = ChatSignals.classifyOverhead(text);
		if (signal == null)
			return;

		onTick(tick);
		switch (signal)
		{
			case KNOCKED_OUT:
				if (budget != null && budget.knockOutTick() == tick)
					setKnockedOutNpc(npcIndex);

				break;
			case WAKING_UP:
				wake(npcIndex);
				break;
			case ANGRY:
				markAttacking(npcIndex, tick);
				break;
			default:
				break;
		}
	}

	/**
	 * Handles an NPC's animation change. The knocked-out NPC playing {@code HUMAN_READY} (808) is the
	 * game snapping it to standing, so the knock-out ends, as with {@code Arghh my head.}.
	 *
	 * @param tick the tick the animation changed on
	 * @param npcIndex the index of the NPC
	 * @param animationId the new animation ID
	 */
	public void onNpcAnimation(int tick, int npcIndex, int animationId)
	{
		if (animationId != AnimationID.HUMAN_READY)
			return;

		onTick(tick);
		wake(npcIndex);
	}

	/**
	 * Handles a hitsplat on the local player: every NPC currently interacting with the player becomes
	 * {@link TargetState#ATTACKING}. Hitsplats carry no source, so this relies on
	 * {@link #onNpcInteracting(int, int, boolean)}.
	 *
	 * @param tick the tick the hitsplat appeared on
	 */
	public void onPlayerHitsplat(int tick)
	{
		onTick(tick);
		for (int npcIndex : new HashSet<>(interactingWithPlayer))
			markAttacking(npcIndex, tick);
	}

	/**
	 * Records whether an NPC is interacting with the local player. The latest value holds until the
	 * next call, so call it whenever it changes, or every tick for the NPCs worth tracking.
	 *
	 * @param tick the current tick
	 * @param npcIndex the index of the NPC
	 * @param targetsPlayer whether the NPC's interacting target is the local player
	 */
	public void onNpcInteracting(int tick, int npcIndex, boolean targetsPlayer)
	{
		onTick(tick);
		if (targetsPlayer)
		{
			interactingWithPlayer.add(npcIndex);
			if (attackingSeenTick.containsKey(npcIndex))
				attackingSeenTick.put(npcIndex, tick);
		}
		else if (interactingWithPlayer.remove(npcIndex) && attackingSeenTick.containsKey(npcIndex))
		{
			attackingSeenTick.put(npcIndex, tick);
		}
	}

	/**
	 * Forgets an NPC that despawned, ending its knock-out if it was knocked out.
	 *
	 * @param npcIndex the index of the despawned NPC
	 */
	public void onDespawn(int npcIndex)
	{
		if (npcIndex == knockedOutNpc)
			clearKnockOut();

		interactingWithPlayer.remove(npcIndex);
		attackingSeenTick.remove(npcIndex);
	}

	/**
	 * Clears everything, e.g. when the player logs out or hops, or the activation gate closes. The
	 * knock-out duration is kept.
	 */
	public void reset()
	{
		clearKnockOut();
		interactingWithPlayer.clear();
		attackingSeenTick.clear();
		currentTick = UNSET;
	}

	/**
	 * The state to draw an NPC in, at the latest tick the tracker has seen.
	 *
	 * @param npcIndex the index of the NPC
	 * @return its state; NPCs the tracker knows nothing about are {@link TargetState#KNOCK_OUT}
	 */
	public TargetState stateOf(int npcIndex)
	{
		if (npcIndex != NO_NPC && npcIndex == knockedOutNpcIndex())
			return pickpocketsLeft() > 0 ? TargetState.SAFE : TargetState.WAKING;

		if (attackingSeenTick.containsKey(npcIndex))
			return TargetState.ATTACKING;

		return TargetState.KNOCK_OUT;
	}

	/**
	 * How many more guaranteed pickpockets will land on the knocked-out target, at the latest tick
	 * the tracker has seen.
	 *
	 * @return 0 to {@link #MAX_PICKPOCKETS}; 0 when nothing is knocked out
	 */
	public int pickpocketsLeft()
	{
		return pickpocketsLeft(currentTick);
	}

	/**
	 * How many more guaranteed pickpockets will land on the knocked-out target if the player clicks
	 * during {@code currentTick} (PLAN §4.3).
	 *
	 * @param currentTick the tick to evaluate at
	 * @return 0 to {@link #MAX_PICKPOCKETS}; 0 when nothing is knocked out
	 */
	public int pickpocketsLeft(int currentTick)
	{
		if (knockedOutNpcIndex() == NO_NPC)
			return 0;

		return budget.pickpocketsLeft(currentTick);
	}

	/**
	 * The index of the knocked-out NPC.
	 *
	 * @return the NPC index, or {@link #NO_NPC} when nothing is knocked out
	 */
	public int knockedOutNpcIndex()
	{
		return budget == null ? NO_NPC : knockedOutNpc;
	}

	/**
	 * The tick the current knock-out landed on.
	 *
	 * @return {@code T}, or {@link #NO_TICK} when nothing is knocked out
	 */
	public int knockOutTick()
	{
		return knockedOutNpcIndex() == NO_NPC ? NO_TICK : budget.knockOutTick();
	}

	/**
	 * The tick the knocked-out NPC wakes on, which the timer counts down to (PLAN §4.4).
	 *
	 * @return {@code T + duration}, or {@link #NO_TICK} when nothing is knocked out
	 */
	public int wakeTick()
	{
		return knockedOutNpcIndex() == NO_NPC ? NO_TICK : budget.wakeTick();
	}

	/**
	 * The latest tick the tracker has seen.
	 *
	 * @return the tick, or {@link Integer#MIN_VALUE} before the first tick and after {@link #reset()}
	 */
	public int currentTick()
	{
		return currentTick;
	}

	private void setKnockedOutNpc(int npcIndex)
	{
		if (npcIndex == NO_NPC)
			return;

		knockedOutNpc = npcIndex;
		interactingWithPlayer.remove(npcIndex);
		attackingSeenTick.remove(npcIndex);
	}

	private void wake(int npcIndex)
	{
		if (npcIndex != NO_NPC && npcIndex == knockedOutNpcIndex())
			clearKnockOut();
	}

	private void markAttacking(int npcIndex, int tick)
	{
		if (npcIndex == NO_NPC || npcIndex == knockedOutNpcIndex())
			return;

		attackingSeenTick.put(npcIndex, tick);
	}

	private void clearKnockOut()
	{
		budget = null;
		knockedOutNpc = NO_NPC;
	}
}
