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

import javax.inject.Inject;
import javax.inject.Singleton;

import com.google.inject.Binder;
import com.google.inject.Provides;

import net.runelite.api.Actor;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.EquipmentInventorySlot;
import net.runelite.api.Hitsplat;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.NPC;
import net.runelite.api.Player;
import net.runelite.api.Skill;
import net.runelite.api.WorldView;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.AnimationChanged;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.HitsplatApplied;
import net.runelite.api.events.InteractingChanged;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.NpcDespawned;
import net.runelite.api.events.NpcSpawned;
import net.runelite.api.events.OverheadTextChanged;
import net.runelite.api.events.StatChanged;
import net.runelite.api.gameval.AnimationID;
import net.runelite.api.gameval.InventoryID;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

/**
 * Entry point of the Better Blackjacking plugin, which helps players blackjack in Pollnivneach.
 *
 * <p>It outlines the target's click box in a colour that says what to do next, counts down to the
 * next knock-out, times the two guaranteed pickpockets, and smooths the target's wake-up animation.
 *
 * <p>The plugin translates RuneLite events into plain calls on the state in {@link PluginStateView}:
 * the {@link KnockoutTracker}, the {@link ActivationGate}, the {@link SubTickClock} and the set of
 * blackjack targets in the scene. The overlays read that state through {@link TargetStateView}. The
 * tick every event is credited to is {@code Client#getTickCount()}.
 */
@PluginDescriptor(
		name = "Better Blackjacking",
		description = "Colour-coded click boxes, a knock-out timer and pickpocket pips"
				+ " for blackjacking in Pollnivneach",
		tags = {"blackjack", "blackjacking", "thieving", "pollnivneach", "bandit", "menaphite", "thug", "knockout",
				"pickpocket", "timer"}
)
public class BetterBlackjackingPlugin extends Plugin
{
	/**
	 * How many ticks from the knock-out's wake tick the target's {@code HUMAN_READY} animation still
	 * counts as it waking (PLAN §5.5).
	 */
	static final int WAKE_WINDOW_TICKS = 2;

	private static final int WEAPON_SLOT = EquipmentInventorySlot.WEAPON.getSlotIdx();

	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private BetterBlackjackingConfig config;

	@Inject
	private PluginStateView state;

	@Inject
	private OutlineOverlay outlineOverlay;

	@Inject
	private TimerOverlay timerOverlay;

	@Inject
	private PickpocketIndicatorOverlay pickpocketIndicatorOverlay;

	@Inject
	private WakeAnimationController wakeAnimationController;

	private DebugLog debugLog;

	private int wakeNpcIndex = KnockoutTracker.NO_NPC;

	private int wakeTick = KnockoutTracker.NO_TICK;

	/**
	 * Supplies the plugin config to Guice.
	 *
	 * @param configManager RuneLite's config manager
	 * @return the config proxy
	 */
	@Provides
	BetterBlackjackingConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(BetterBlackjackingConfig.class);
	}

	/**
	 * Binds {@link TargetStateView} to the single {@link PluginStateView} the plugin updates, so the
	 * overlays draw the plugin's state.
	 *
	 * @param binder the plugin's Guice binder
	 */
	@Override
	public void configure(Binder binder)
	{
		binder.bind(PluginStateView.class).in(Singleton.class);
		binder.bind(TargetStateView.class).to(PluginStateView.class);
	}

	/**
	 * Registers the overlays, sets the knock-out duration, and reads the player's equipment, Thieving
	 * level, location and the NPCs already in the scene on the client thread.
	 */
	@Override
	protected void startUp()
	{
		debugLog = new DebugLog(config, client::getTickCount);
		state.tracker().setKnockOutTicks(config.knockOutTicks());
		overlayManager.add(outlineOverlay);
		overlayManager.add(timerOverlay);
		overlayManager.add(pickpocketIndicatorOverlay);
		clientThread.invoke(this::readClientState);
	}

	/**
	 * Removes the overlays and clears all state on the client thread.
	 */
	@Override
	protected void shutDown()
	{
		overlayManager.remove(outlineOverlay);
		overlayManager.remove(timerOverlay);
		overlayManager.remove(pickpocketIndicatorOverlay);
		clientThread.invoke(this::resetState);
	}

	/**
	 * Advances the clock and the tracker, moves the gate to the player's location, and polls whether
	 * each target is interacting with the player, which keeps the ATTACKING exit rule current.
	 *
	 * @param event the tick
	 */
	@Subscribe
	public void onGameTick(GameTick event)
	{
		int tick = client.getTickCount();
		KnockoutTracker tracker = state.tracker();
		state.clock().onTick();
		tracker.onTick(tick);

		Player player = client.getLocalPlayer();
		if (player == null)
			return;

		updateGate(state.gate().withLocation(locationOf(player)));
		for (NPC npc : state.targets())
			tracker.onNpcInteracting(tick, npc.getIndex(), npc.getInteracting() == player);
	}

	/**
	 * Feeds game messages to the tracker, crediting them to the NPC the player is interacting with.
	 * A knock-out success also records the knock-out tick in the debug log, after logging the chat
	 * line and before logging the knock-out line, as {@link DebugLog} requires.
	 *
	 * @param event the chat message
	 */
	@Subscribe
	public void onChatMessage(ChatMessage event)
	{
		ChatMessageType type = event.getType();
		if (type != ChatMessageType.GAMEMESSAGE && type != ChatMessageType.SPAM)
			return;

		int tick = client.getTickCount();
		String message = event.getMessage();
		ChatSignals.Message signal = ChatSignals.classifyMessage(message);
		debugLog.chat(type, signal != null, message);

		NPC interacting = localInteractingNpc();
		int interactingIndex = interacting == null ? KnockoutTracker.NO_NPC : interacting.getIndex();
		state.tracker().onChat(tick, type, message, interactingIndex);
		if (signal == ChatSignals.Message.KNOCKOUT_SUCCESS)
			logKnockout(tick, interacting);

		rememberKnockout();
	}

	/**
	 * Feeds a target's overhead text to the tracker.
	 *
	 * @param event the overhead text change
	 */
	@Subscribe
	public void onOverheadTextChanged(OverheadTextChanged event)
	{
		NPC npc = trackedNpc(event.getActor());
		if (npc == null)
			return;

		if (debugLog.isEnabled())
			debugLog.overhead(describe(npc), event.getOverheadText());

		state.tracker().onOverhead(client.getTickCount(), npc.getIndex(), event.getOverheadText());
		rememberKnockout();
	}

	/**
	 * Feeds a target's animation to the tracker, and calls {@link #onTargetWoke(NPC)} when the last
	 * knocked-out target plays {@code HUMAN_READY} within {@link #WAKE_WINDOW_TICKS} of its wake tick.
	 *
	 * @param event the animation change
	 */
	@Subscribe
	public void onAnimationChanged(AnimationChanged event)
	{
		Actor actor = event.getActor();
		if (actor != null && actor == client.getLocalPlayer())
		{
			if (debugLog.isEnabled())
				debugLog.animation(DebugLog.PLAYER, actor.getAnimation(), actor.getPoseAnimation());

			return;
		}

		NPC npc = trackedNpc(actor);
		if (npc == null)
			return;

		int tick = client.getTickCount();
		int animation = npc.getAnimation();
		if (debugLog.isEnabled())
			debugLog.animation(describe(npc), animation, npc.getPoseAnimation());

		state.tracker().onNpcAnimation(tick, npc.getIndex(), animation);
		if (animation == AnimationID.HUMAN_READY && npc.getIndex() == wakeNpcIndex
				&& Math.abs(tick - wakeTick) <= WAKE_WINDOW_TICKS)
		{
			forgetWake();
			onTargetWoke(npc);
		}
	}

	/**
	 * Tells the tracker the player was hit, which makes every target interacting with them ATTACKING.
	 *
	 * @param event the hitsplat
	 */
	@Subscribe
	public void onHitsplatApplied(HitsplatApplied event)
	{
		Player player = client.getLocalPlayer();
		if (player == null || event.getActor() != player)
			return;

		if (debugLog.isEnabled())
		{
			Hitsplat hitsplat = event.getHitsplat();
			debugLog.hitsplat(DebugLog.PLAYER, hitsplat == null ? 0 : hitsplat.getAmount());
		}

		state.tracker().onPlayerHitsplat(client.getTickCount());
	}

	/**
	 * Records whether a target is now interacting with the player.
	 *
	 * @param event the interaction change
	 */
	@Subscribe
	public void onInteractingChanged(InteractingChanged event)
	{
		Player player = client.getLocalPlayer();
		Actor source = event.getSource();
		if (player != null && source == player)
		{
			if (debugLog.isEnabled())
				debugLog.interacting(DebugLog.PLAYER, describe(event.getTarget()));

			return;
		}

		NPC npc = trackedNpc(source);
		if (npc == null)
			return;

		if (debugLog.isEnabled())
			debugLog.interacting(describe(npc), describe(event.getTarget()));

		boolean targetsPlayer = player != null && event.getTarget() == player;
		state.tracker().onNpcInteracting(client.getTickCount(), npc.getIndex(), targetsPlayer);
	}

	/**
	 * Starts tracking a blackjack target that spawned.
	 *
	 * @param event the spawn
	 */
	@Subscribe
	public void onNpcSpawned(NpcSpawned event)
	{
		NPC npc = event.getNpc();
		if (state.addTarget(npc) && debugLog.isEnabled())
			debugLog.log("spawn %s", describe(npc));
	}

	/**
	 * Stops tracking an NPC that despawned and tells the tracker, which ends its knock-out.
	 *
	 * @param event the despawn
	 */
	@Subscribe
	public void onNpcDespawned(NpcDespawned event)
	{
		NPC npc = event.getNpc();
		if (npc == null)
			return;

		if (state.removeTarget(npc) && debugLog.isEnabled())
			debugLog.despawn(describe(npc));

		if (npc.getIndex() == wakeNpcIndex)
			forgetWake();

		state.tracker().onDespawn(npc.getIndex());
	}

	/**
	 * Updates the gate's weapon when the equipment changes.
	 *
	 * @param event the container change
	 */
	@Subscribe
	public void onItemContainerChanged(ItemContainerChanged event)
	{
		if (event.getContainerId() != InventoryID.WORN)
			return;

		updateGate(state.gate().withWeaponItemId(weaponIn(event.getItemContainer())));
	}

	/**
	 * Updates the gate's boosted Thieving level.
	 *
	 * @param event the stat change
	 */
	@Subscribe
	public void onStatChanged(StatChanged event)
	{
		if (event.getSkill() != Skill.THIEVING)
			return;

		updateGate(state.gate().withThievingLevel(event.getBoostedLevel()));
	}

	/**
	 * Clears everything on logout or hop, and re-reads the weapon and Thieving level on login.
	 *
	 * @param event the game state change
	 */
	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		switch (event.getGameState())
		{
			case LOGIN_SCREEN:
			case HOPPING:
				resetState();
				break;
			case LOGGED_IN:
				readEquipmentAndLevel();
				break;
			default:
				break;
		}
	}

	/**
	 * Applies a new knock-out duration.
	 *
	 * @param event the config change
	 */
	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (!BetterBlackjackingConfig.GROUP.equals(event.getGroup()))
			return;

		state.tracker().setKnockOutTicks(config.knockOutTicks());
	}

	/**
	 * Called once when the last knocked-out target plays {@code HUMAN_READY} (the game snapping it to
	 * standing) within {@link #WAKE_WINDOW_TICKS} ticks of its wake tick. It replaces that animation with
	 * the configured wake-up animation (PLAN §5.5) through {@link WakeAnimationController}.
	 *
	 * @param npc the target that woke
	 */
	void onTargetWoke(NPC npc)
	{
		wakeAnimationController.play(npc);
	}

	private void readClientState()
	{
		Player player = client.getLocalPlayer();
		WorldPoint location = player == null ? null : locationOf(player);
		updateGate(ActivationGate.of(location, wieldedWeapon(), client.getBoostedSkillLevel(Skill.THIEVING)));

		WorldView view = client.getTopLevelWorldView();
		if (view == null)
			return;

		for (NPC npc : view.npcs())
			state.addTarget(npc);
	}

	private void readEquipmentAndLevel()
	{
		ActivationGate armed = state.gate().withWeaponItemId(wieldedWeapon());
		updateGate(armed.withThievingLevel(client.getBoostedSkillLevel(Skill.THIEVING)));
	}

	private void resetState()
	{
		state.clear();
		forgetWake();
		if (debugLog != null)
			debugLog.clearKnockout();
	}

	private void updateGate(ActivationGate next)
	{
		ActivationGate previous = state.gate();
		if (next == previous)
			return;

		if (state.setGate(next))
		{
			forgetWake();
			debugLog.clearKnockout();
		}

		if (debugLog.isEnabled())
			debugLog.log("gate %s", next);
	}

	private void rememberKnockout()
	{
		KnockoutTracker tracker = state.tracker();
		int index = tracker.knockedOutNpcIndex();
		if (index == KnockoutTracker.NO_NPC)
			return;

		wakeNpcIndex = index;
		wakeTick = tracker.wakeTick();
	}

	private void forgetWake()
	{
		wakeNpcIndex = KnockoutTracker.NO_NPC;
		wakeTick = KnockoutTracker.NO_TICK;
	}

	private void logKnockout(int tick, NPC interacting)
	{
		debugLog.setKnockoutTick(tick);
		if (!debugLog.isEnabled())
			return;

		KnockoutTracker tracker = state.tracker();
		NPC target = state.targetByIndex(tracker.knockedOutNpcIndex());
		int duration = tracker.knockOutTicks();
		debugLog.knockout(describe(target), describe(interacting), target == null ? -1 : target.getAnimation(),
				target == null ? -1 : target.getPoseAnimation(), duration, tick + duration);
	}

	private NPC trackedNpc(Actor actor)
	{
		if (!(actor instanceof NPC))
			return null;

		NPC npc = (NPC) actor;
		return state.isTarget(npc) ? npc : null;
	}

	private NPC localInteractingNpc()
	{
		Player player = client.getLocalPlayer();
		if (player == null)
			return null;

		Actor interacting = player.getInteracting();
		return interacting instanceof NPC ? (NPC) interacting : null;
	}

	private WorldPoint locationOf(Player player)
	{
		LocalPoint local = player.getLocalLocation();
		return local == null ? null : WorldPoint.fromLocalInstance(client, local);
	}

	private int wieldedWeapon()
	{
		return weaponIn(client.getItemContainer(InventoryID.WORN));
	}

	private static int weaponIn(ItemContainer equipment)
	{
		if (equipment == null)
			return ActivationGate.NO_WEAPON;

		Item weapon = equipment.getItem(WEAPON_SLOT);
		return weapon == null || weapon.getId() < 0 ? ActivationGate.NO_WEAPON : weapon.getId();
	}

	private String describe(Actor actor)
	{
		if (actor == null)
			return DebugLog.NONE;

		if (actor == client.getLocalPlayer())
			return DebugLog.PLAYER;

		if (actor instanceof NPC)
		{
			NPC npc = (NPC) actor;
			return DebugLog.describe(npc.getName(), npc.getIndex(), npc.getId());
		}

		return actor.getName();
	}
}
