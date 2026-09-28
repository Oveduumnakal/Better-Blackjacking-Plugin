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

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import javax.inject.Inject;

import net.runelite.api.NPC;

/**
 * The plugin's state, and the {@link TargetStateView} the overlays draw from.
 *
 * <p>It holds one {@link KnockoutTracker}, one {@link SubTickClock}, the current
 * {@link ActivationGate} and every blackjack target in the scene. The plugin's event handlers update
 * them; the overlays only read through {@link TargetStateView}. Guice binds it as a singleton, so
 * the plugin and every overlay share one instance. Everything runs on the client thread.
 *
 * <p>{@link #eligibleTargets()} is called every frame, so the eligible targets are cached and the
 * cache is rebuilt only after the gate or the set of targets changes.
 */
class PluginStateView implements TargetStateView
{
	private final BetterBlackjackingConfig config;

	private final SubTickClock clock;

	private final KnockoutTracker tracker = new KnockoutTracker();

	private final Set<NPC> targets = new LinkedHashSet<>();

	private final Collection<NPC> targetsView = Collections.unmodifiableSet(targets);

	private ActivationGate gate = ActivationGate.CLOSED;

	private List<NPC> eligible;

	/**
	 * Creates the state with a clock that reads {@link System#nanoTime()}.
	 *
	 * @param config the plugin config, which gives the state colours
	 */
	@Inject
	PluginStateView(BetterBlackjackingConfig config)
	{
		this(config, new SubTickClock());
	}

	/**
	 * Creates the state with its own clock, so tests can control time.
	 *
	 * @param config the plugin config, which gives the state colours
	 * @param clock the clock the countdown interpolates with
	 */
	PluginStateView(BetterBlackjackingConfig config, SubTickClock clock)
	{
		this.config = config;
		this.clock = clock;
	}

	/**
	 * The knock-out tracker the plugin feeds events into.
	 *
	 * @return the tracker
	 */
	KnockoutTracker tracker()
	{
		return tracker;
	}

	/**
	 * The clock the plugin ticks on every {@code GameTick}.
	 *
	 * @return the clock
	 */
	SubTickClock clock()
	{
		return clock;
	}

	/**
	 * The current activation gate.
	 *
	 * @return the gate; {@link ActivationGate#CLOSED} before the plugin has read the player's state
	 */
	ActivationGate gate()
	{
		return gate;
	}

	/**
	 * Replaces the activation gate. When this closes an open gate, the tracker is reset, since
	 * PLAN §4.2 clears everything when the gate closes.
	 *
	 * @param next the new gate
	 * @return {@code true} if the gate went from active to inactive
	 */
	boolean setGate(ActivationGate next)
	{
		if (next == gate)
			return false;

		boolean closed = gate.isActive() && !next.isActive();
		gate = next;
		eligible = null;
		if (closed)
			tracker.reset();

		return closed;
	}

	/**
	 * Starts tracking an NPC if it is a {@link BlackjackTarget}, whether or not it is eligible now.
	 *
	 * @param npc the NPC that spawned or was found in the scene
	 * @return {@code true} if the NPC is a target and wasn't tracked yet
	 */
	boolean addTarget(NPC npc)
	{
		if (npc == null || BlackjackTarget.forNpcId(npc.getId()) == null || !targets.add(npc))
			return false;

		eligible = null;
		return true;
	}

	/**
	 * Stops tracking an NPC.
	 *
	 * @param npc the NPC that despawned
	 * @return {@code true} if the NPC was tracked
	 */
	boolean removeTarget(NPC npc)
	{
		if (!targets.remove(npc))
			return false;

		eligible = null;
		return true;
	}

	/**
	 * Whether an NPC is a tracked blackjack target.
	 *
	 * @param npc the NPC
	 * @return {@code true} if it is in the target set
	 */
	boolean isTarget(NPC npc)
	{
		return targets.contains(npc);
	}

	/**
	 * Every tracked blackjack target in the scene, eligible or not.
	 *
	 * @return an unmodifiable live view of the targets
	 */
	Collection<NPC> targets()
	{
		return targetsView;
	}

	/**
	 * Finds a tracked target by its NPC index.
	 *
	 * @param npcIndex the NPC index, as from {@code NPC#getIndex()}
	 * @return the target, or {@code null} if no tracked target has that index
	 */
	NPC targetByIndex(int npcIndex)
	{
		if (npcIndex == KnockoutTracker.NO_NPC)
			return null;

		for (NPC npc : targets)
		{
			if (npc.getIndex() == npcIndex)
				return npc;
		}

		return null;
	}

	/**
	 * Forgets everything: the targets, the gate, the tracker's state and the clock. The tracker's
	 * knock-out duration is kept.
	 */
	void clear()
	{
		targets.clear();
		gate = ActivationGate.CLOSED;
		eligible = null;
		tracker.reset();
		clock.reset();
	}

	/**
	 * Whether the activation gate is open.
	 *
	 * @return whether the plugin is active
	 */
	@Override
	public boolean isActive()
	{
		return gate.isActive();
	}

	/**
	 * The tracked targets the gate says are eligible, from a cache rebuilt only after the gate or
	 * the target set changes.
	 *
	 * @return the eligible targets; empty while the plugin is inactive
	 */
	@Override
	public Collection<NPC> eligibleTargets()
	{
		if (!gate.isActive())
			return Collections.emptyList();

		if (eligible == null)
		{
			List<NPC> rebuilt = new ArrayList<>(targets.size());
			for (NPC npc : targets)
			{
				if (gate.isEligibleNpc(npc.getId()))
					rebuilt.add(npc);
			}

			eligible = Collections.unmodifiableList(rebuilt);
		}

		return eligible;
	}

	/**
	 * The tracker's state for the NPC's index.
	 *
	 * @param npc a target
	 * @return its state; {@link TargetState#KNOCK_OUT} for {@code null}
	 */
	@Override
	public TargetState stateOf(NPC npc)
	{
		return npc == null ? TargetState.KNOCK_OUT : tracker.stateOf(npc.getIndex());
	}

	/**
	 * The tracker's pickpockets left at its latest tick.
	 *
	 * @return 0 to {@link KnockoutTracker#MAX_PICKPOCKETS}
	 */
	@Override
	public int pickpocketsLeft()
	{
		return tracker.pickpocketsLeft();
	}

	/**
	 * The time until the tracker's wake tick, interpolated by the clock.
	 *
	 * @return the time left in milliseconds; 0 when nothing is knocked out
	 */
	@Override
	public long remainingMillis()
	{
		int wakeTick = tracker.wakeTick();
		if (wakeTick == KnockoutTracker.NO_TICK)
			return 0;

		return clock.remainingMillis(wakeTick, tracker.currentTick());
	}

	/**
	 * The tracker's knock-out duration in milliseconds.
	 *
	 * @return {@code knockOutTicks × 600}
	 */
	@Override
	public long durationMillis()
	{
		return tracker.knockOutTicks() * SubTickClock.TICK_MILLIS;
	}

	/**
	 * The configured colour for a state.
	 *
	 * @param state a target state; {@code null} counts as {@link TargetState#KNOCK_OUT}
	 * @return the colour
	 */
	@Override
	public Color colorOf(TargetState state)
	{
		if (state == null)
			return config.knockOutColor();

		switch (state)
		{
			case SAFE:
				return config.safeColor();
			case WAKING:
				return config.wakingColor();
			case ATTACKING:
				return config.attackingColor();
			case KNOCK_OUT:
			default:
				return config.knockOutColor();
		}
	}
}
