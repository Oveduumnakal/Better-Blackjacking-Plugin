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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javax.inject.Inject;

import com.google.inject.Guice;
import com.google.inject.testing.fieldbinder.Bind;
import com.google.inject.testing.fieldbinder.BoundFieldModule;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import net.runelite.api.Actor;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.EquipmentInventorySlot;
import net.runelite.api.GameState;
import net.runelite.api.Hitsplat;
import net.runelite.api.IndexedObjectSet;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.NPC;
import net.runelite.api.Player;
import net.runelite.api.Skill;
import net.runelite.api.WorldView;
import net.runelite.api.coords.LocalPoint;
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
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.NpcID;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.ui.overlay.OverlayManager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Drives {@link BetterBlackjackingPlugin}'s event handlers through a real {@code startUp()} against
 * Guice-bound Mockito mocks, and checks the result through the {@link TargetStateView} the overlays
 * are given. {@link ClientThread} runs work inline, the config answers with its real defaults, and
 * the player stands in Pollnivneach with a blackjack and Thieving 70 unless a test changes that.
 */
public class BetterBlackjackingPluginHandlersTest
{
	private static final int POLLNIVNEACH_BASE_X = 3328;

	private static final int POLLNIVNEACH_BASE_Y = 2944;

	private static final int ELSEWHERE_BASE_X = 3200;

	private static final int TILE = 128;

	private static final int TOP_LEVEL = -1;

	private static final String KNOCKOUT = "You smack the bandit over the head and render them unconscious.";

	@Mock
	@Bind
	private Client client;

	@Mock
	@Bind
	private ClientThread clientThread;

	@Mock
	@Bind
	private OverlayManager overlayManager;

	@Bind
	private final BetterBlackjackingConfig config = mock(BetterBlackjackingConfig.class, Answers.CALLS_REAL_METHODS);

	@Inject
	private RecordingPlugin plugin;

	@Inject
	private TargetStateView view;

	@Inject
	private PluginStateView state;

	@Mock
	private Player player;

	@Mock
	private WorldView worldView;

	private final List<NPC> scene = new ArrayList<>();

	private int tick = 100;

	private int baseX = POLLNIVNEACH_BASE_X;

	private int thievingLevel = 70;

	private int weapon = ItemID.BLACKJACK_OAK;

	private AutoCloseable mocks;

	/** A plugin that records every call to its wake hook. */
	public static class RecordingPlugin extends BetterBlackjackingPlugin
	{
		private final List<NPC> woke = new ArrayList<>();

		@Override
		void onTargetWoke(NPC npc)
		{
			woke.add(npc);
		}
	}

	@Before
	@SuppressWarnings("unchecked")
	public void setUp()
	{
		mocks = MockitoAnnotations.openMocks(this);
		Guice.createInjector(BoundFieldModule.of(this), binder -> new BetterBlackjackingPlugin().configure(binder))
				.injectMembers(this);

		doAnswer(invocation ->
		{
			invocation.<Runnable>getArgument(0).run();
			return null;
		})
				.when(clientThread)
				.invoke(any(Runnable.class));

		when(client.getTickCount()).thenAnswer(invocation -> tick);
		when(client.getGameState()).thenReturn(GameState.LOGGED_IN);
		when(client.getLocalPlayer()).thenReturn(player);
		when(client.getWorldView(anyInt())).thenReturn(worldView);
		when(client.getTopLevelWorldView()).thenReturn(worldView);
		when(client.getBoostedSkillLevel(Skill.THIEVING)).thenAnswer(invocation -> thievingLevel);
		when(client.getItemContainer(InventoryID.WORN)).thenAnswer(invocation -> equipment(weapon));
		when(worldView.getBaseX()).thenAnswer(invocation -> baseX);
		when(worldView.getBaseY()).thenReturn(POLLNIVNEACH_BASE_Y);
		when(player.getLocalLocation()).thenReturn(new LocalPoint(10 * TILE, 10 * TILE, TOP_LEVEL));

		IndexedObjectSet<NPC> npcs = mock(IndexedObjectSet.class);
		when(npcs.iterator()).thenAnswer(invocation -> new ArrayList<>(scene).iterator());
		doReturn(npcs).when(worldView)
				.npcs();
	}

	@After
	public void tearDown() throws Exception
	{
		mocks.close();
	}

	@Test
	public void providesTheConfigFromTheConfigManager()
	{
		ConfigManager configManager = mock(ConfigManager.class);
		when(configManager.getConfig(BetterBlackjackingConfig.class)).thenReturn(config);

		assertSame(config, plugin.provideConfig(configManager));
	}

	@Test
	public void overlaysShareThePluginState()
	{
		assertTrue(view instanceof PluginStateView);
		assertSame(state, view);
	}

	@Test
	public void startUpRegistersOverlaysAndReadsTheClient()
	{
		NPC bandit = npc(1, NpcID.FEUD_ARABIAN_GUARD2_1);
		NPC villager = npc(2, NpcID.FEUD_VILLAGER_1_1);
		scene.add(bandit);
		scene.add(villager);

		plugin.startUp();

		verify(overlayManager).add(any(OutlineOverlay.class));
		verify(overlayManager).add(any(TimerOverlay.class));
		verify(overlayManager).add(any(PickpocketIndicatorOverlay.class));
		assertTrue(view.isActive());
		assertEquals(Collections.singletonList(bandit), new ArrayList<>(view.eligibleTargets()));
		assertEquals(5 * SubTickClock.TICK_MILLIS, view.durationMillis());
	}

	@Test
	public void shutDownRemovesOverlaysAndClearsState()
	{
		NPC bandit = startWithBandit();
		knockOut(bandit);

		plugin.shutDown();

		verify(overlayManager).remove(any(OutlineOverlay.class));
		verify(overlayManager).remove(any(TimerOverlay.class));
		verify(overlayManager).remove(any(PickpocketIndicatorOverlay.class));
		assertFalse(view.isActive());
		assertTrue(view.eligibleTargets().isEmpty());
		assertEquals(TargetState.KNOCK_OUT, view.stateOf(bandit));
	}

	@Test
	public void knockOutChatMakesTheInteractingTargetSafe()
	{
		NPC bandit = startWithBandit();
		NPC other = spawn(2, NpcID.FEUD_ARABIAN_GUARD2_2);

		knockOut(bandit);

		assertEquals(TargetState.SAFE, view.stateOf(bandit));
		assertEquals(TargetState.KNOCK_OUT, view.stateOf(other));
		assertEquals(2, view.pickpocketsLeft());
		assertTrue(view.remainingMillis() > 0);
	}

	@Test
	public void knockOutWithoutInteractingIsNamedByTheZzzOverhead()
	{
		NPC bandit = startWithBandit();
		chat(KNOCKOUT);
		assertEquals(TargetState.KNOCK_OUT, view.stateOf(bandit));

		overhead(bandit, "Zzzzzz");

		assertEquals(TargetState.SAFE, view.stateOf(bandit));
	}

	@Test
	public void pickpocketChatReachesTheTracker()
	{
		NPC bandit = startWithBandit();
		knockOut(bandit);
		assertEquals(2, view.pickpocketsLeft());

		chat("You attempt to pick the bandit's pocket.");

		assertEquals(1, view.pickpocketsLeft());
	}

	@Test
	public void wakeOverheadEndsTheKnockOut()
	{
		NPC bandit = startWithBandit();
		knockOut(bandit);

		overhead(bandit, "Arghh my head.");

		assertEquals(TargetState.KNOCK_OUT, view.stateOf(bandit));
	}

	@Test
	public void wakeTickEndsTheKnockOut()
	{
		NPC bandit = startWithBandit();
		knockOut(bandit);
		for (int i = 0; i < 4; i++)
			gameTick();

		assertTrue(view.stateOf(bandit).isKnockedOut());
		gameTick();

		assertEquals(TargetState.KNOCK_OUT, view.stateOf(bandit));
		assertEquals(0, view.remainingMillis());
	}

	@Test
	public void failedKnockOutMakesTheTargetAttacking()
	{
		NPC bandit = startWithBandit();
		when(player.getInteracting()).thenReturn(bandit);

		chat("Your blow only glances off the bandit's head.");

		assertEquals(TargetState.ATTACKING, view.stateOf(bandit));
	}

	@Test
	public void angryOverheadMakesTheTargetAttacking()
	{
		NPC bandit = startWithBandit();

		overhead(bandit, "I'll kill you for that!");

		assertEquals(TargetState.ATTACKING, view.stateOf(bandit));
	}

	@Test
	public void hitsplatOnThePlayerMakesInteractingTargetsAttacking()
	{
		NPC bandit = startWithBandit();
		NPC bystander = spawn(2, NpcID.FEUD_ARABIAN_GUARD1_1);
		plugin.onInteractingChanged(new InteractingChanged(bandit, player));

		hitsplatOn(bystander);
		assertEquals(TargetState.KNOCK_OUT, view.stateOf(bandit));
		hitsplatOn(player);

		assertEquals(TargetState.ATTACKING, view.stateOf(bandit));
		assertEquals(TargetState.KNOCK_OUT, view.stateOf(bystander));
	}

	@Test
	public void attackingEndsOnceTheTargetStopsInteracting()
	{
		NPC bandit = startWithBandit();
		when(bandit.getInteracting()).thenReturn(player);
		gameTick();
		hitsplatOn(player);
		gameTick();
		assertEquals(TargetState.ATTACKING, view.stateOf(bandit));

		when(bandit.getInteracting()).thenReturn(null);
		gameTick();
		gameTick();
		assertEquals(TargetState.ATTACKING, view.stateOf(bandit));
		gameTick();

		assertEquals(TargetState.KNOCK_OUT, view.stateOf(bandit));
	}

	@Test
	public void wakeAnimationEndsTheKnockOutAndCallsTheHookOnce()
	{
		NPC bandit = startWithBandit();
		knockOut(bandit);
		for (int i = 0; i < 5; i++)
			gameTick();

		animate(bandit, AnimationID.HUMAN_READY);
		animate(bandit, AnimationID.HUMAN_READY);

		assertEquals(TargetState.KNOCK_OUT, view.stateOf(bandit));
		assertEquals(Collections.singletonList(bandit), plugin.woke);
	}

	@Test
	public void wakeHookIgnoresOtherNpcsAndAnimationsOutsideTheWindow()
	{
		NPC bandit = startWithBandit();
		NPC other = spawn(2, NpcID.FEUD_ARABIAN_GUARD2_2);
		knockOut(bandit);

		animate(bandit, AnimationID.HUMAN_UNCONSCIOUS);
		for (int i = 0; i < 5; i++)
			gameTick();

		animate(other, AnimationID.HUMAN_READY);
		assertTrue(plugin.woke.isEmpty());
		for (int i = 0; i < 3; i++)
			gameTick();

		animate(bandit, AnimationID.HUMAN_READY);

		assertTrue(plugin.woke.isEmpty());
	}

	@Test
	public void gateOpensWithABlackjackInPollnivneach()
	{
		weapon = ActivationGate.NO_WEAPON;
		NPC bandit = startWithBandit();
		assertFalse(view.isActive());
		assertTrue(view.eligibleTargets().isEmpty());

		equip(ItemID.ROGUETRADER_BJ_MAPLE);

		assertTrue(view.isActive());
		assertEquals(Collections.singletonList(bandit), new ArrayList<>(view.eligibleTargets()));
	}

	@Test
	public void unequippingTheBlackjackClosesTheGateAndResetsTheTracker()
	{
		NPC bandit = startWithBandit();
		knockOut(bandit);

		equip(ActivationGate.NO_WEAPON);

		assertFalse(view.isActive());
		assertTrue(view.eligibleTargets().isEmpty());
		assertEquals(TargetState.KNOCK_OUT, view.stateOf(bandit));

		equip(ItemID.BLACKJACK_WILLOW);

		assertTrue(view.isActive());
		assertEquals(TargetState.KNOCK_OUT, view.stateOf(bandit));
		assertEquals(0, view.pickpocketsLeft());
	}

	@Test
	public void leavingPollnivneachClosesTheGateAndResetsTheTracker()
	{
		NPC bandit = startWithBandit();
		knockOut(bandit);

		baseX = ELSEWHERE_BASE_X;
		gameTick();

		assertFalse(view.isActive());
		assertEquals(TargetState.KNOCK_OUT, view.stateOf(bandit));

		baseX = POLLNIVNEACH_BASE_X;
		gameTick();

		assertTrue(view.isActive());
		assertEquals(TargetState.KNOCK_OUT, view.stateOf(bandit));
	}

	@Test
	public void eligibilityFollowsTheBoostedThievingLevel()
	{
		thievingLevel = 50;
		NPC bearded = startWithBandit();
		NPC cleanShaven = spawn(2, NpcID.FEUD_ARABIAN_GUARD1_1);
		NPC thug = spawn(3, NpcID.FEUD_EGYPTIAN_DOORMAN_2);
		assertEquals(Collections.singletonList(bearded), new ArrayList<>(view.eligibleTargets()));

		plugin.onStatChanged(new StatChanged(Skill.THIEVING, 0, 50, 55));
		assertEquals(Arrays.asList(bearded, cleanShaven), new ArrayList<>(view.eligibleTargets()));

		plugin.onStatChanged(new StatChanged(Skill.ATTACK, 0, 99, 99));
		assertEquals(Arrays.asList(bearded, cleanShaven), new ArrayList<>(view.eligibleTargets()));

		plugin.onStatChanged(new StatChanged(Skill.THIEVING, 0, 50, 65));
		assertEquals(Arrays.asList(bearded, cleanShaven, thug), new ArrayList<>(view.eligibleTargets()));
	}

	@Test
	public void spawnAddsOnlyBlackjackTargets()
	{
		startWithBandit();
		NPC clean = spawn(2, NpcID.FEUD_ARABIAN_GUARD1_2);
		spawn(3, NpcID.FEUD_VILLAGER_1_1);

		assertEquals(2, view.eligibleTargets().size());
		assertTrue(view.eligibleTargets().contains(clean));
	}

	@Test
	public void despawnRemovesTheTargetAndEndsItsKnockOut()
	{
		NPC bandit = startWithBandit();
		knockOut(bandit);

		plugin.onNpcDespawned(new NpcDespawned(bandit));

		assertTrue(view.eligibleTargets().isEmpty());
		assertEquals(0, view.pickpocketsLeft());
		assertEquals(0, view.remainingMillis());
	}

	@Test
	public void logoutAndHopResetAndLoginRereadsTheWeaponAndLevel()
	{
		NPC bandit = startWithBandit();
		knockOut(bandit);

		gameState(GameState.HOPPING);

		assertFalse(view.isActive());
		assertTrue(view.eligibleTargets().isEmpty());
		assertEquals(0, view.pickpocketsLeft());

		thievingLevel = 40;
		gameState(GameState.LOGGED_IN);
		gameTick();
		spawn(1, NpcID.FEUD_ARABIAN_GUARD2_1);

		assertTrue(view.isActive());
		assertTrue("Thieving 40 is below every target", view.eligibleTargets().isEmpty());

		gameState(GameState.LOGIN_SCREEN);
		assertFalse(view.isActive());
	}

	@Test
	public void configChangeSetsTheKnockOutDuration()
	{
		startWithBandit();
		when(config.knockOutTicks()).thenReturn(7);

		ConfigChanged other = new ConfigChanged();
		other.setGroup("someotherplugin");
		plugin.onConfigChanged(other);
		assertEquals(5 * SubTickClock.TICK_MILLIS, view.durationMillis());

		ConfigChanged ours = new ConfigChanged();
		ours.setGroup(BetterBlackjackingConfig.GROUP);
		ours.setKey(BetterBlackjackingConfig.KEY_KNOCK_OUT_TICKS);
		plugin.onConfigChanged(ours);

		assertEquals(7 * SubTickClock.TICK_MILLIS, view.durationMillis());
	}

	@Test
	public void debugLoggingDoesNotChangeTheOutcome()
	{
		when(config.debugLogging()).thenReturn(true);
		NPC bandit = startWithBandit();
		plugin.onInteractingChanged(new InteractingChanged(player, bandit));
		plugin.onInteractingChanged(new InteractingChanged(bandit, null));
		animate(player, 401);
		knockOut(bandit);
		overhead(bandit, "Zzzzzz");
		hitsplatOn(player);
		chat("You attempt to pick the bandit's pocket.");

		assertEquals(TargetState.SAFE, view.stateOf(bandit));

		plugin.onNpcDespawned(new NpcDespawned(bandit));
		assertTrue(view.eligibleTargets().isEmpty());
	}

	private NPC startWithBandit()
	{
		NPC bandit = npc(1, NpcID.FEUD_ARABIAN_GUARD2_1);
		scene.add(bandit);
		plugin.startUp();
		gameTick();
		return bandit;
	}

	private void knockOut(NPC target)
	{
		when(player.getInteracting()).thenReturn(target);
		chat(KNOCKOUT);
	}

	private static NPC npc(int index, int id)
	{
		NPC npc = mock(NPC.class);
		when(npc.getIndex()).thenReturn(index);
		when(npc.getId()).thenReturn(id);
		when(npc.getName()).thenReturn("Bandit");
		return npc;
	}

	private NPC spawn(int index, int id)
	{
		NPC npc = npc(index, id);
		plugin.onNpcSpawned(new NpcSpawned(npc));
		return npc;
	}

	private static ItemContainer equipment(int weaponItemId)
	{
		ItemContainer container = mock(ItemContainer.class);
		Item item = weaponItemId < 0 ? null : new Item(weaponItemId, 1);
		when(container.getItem(EquipmentInventorySlot.WEAPON.getSlotIdx())).thenReturn(item);
		return container;
	}

	private void equip(int weaponItemId)
	{
		weapon = weaponItemId;
		plugin.onItemContainerChanged(new ItemContainerChanged(InventoryID.WORN, equipment(weaponItemId)));
	}

	private void gameTick()
	{
		tick++;
		plugin.onGameTick(new GameTick());
	}

	private void chat(String message)
	{
		plugin.onChatMessage(new ChatMessage(null, ChatMessageType.SPAM, "", message, "", 0));
	}

	private void overhead(NPC npc, String text)
	{
		plugin.onOverheadTextChanged(new OverheadTextChanged(npc, text));
	}

	private void animate(NPC npc, int animationId)
	{
		when(npc.getAnimation()).thenReturn(animationId);
		AnimationChanged event = new AnimationChanged();
		event.setActor(npc);
		plugin.onAnimationChanged(event);
	}

	private void animate(Player actor, int animationId)
	{
		when(actor.getAnimation()).thenReturn(animationId);
		AnimationChanged event = new AnimationChanged();
		event.setActor(actor);
		plugin.onAnimationChanged(event);
	}

	private void hitsplatOn(Actor actor)
	{
		Hitsplat hitsplat = mock(Hitsplat.class);
		when(hitsplat.getAmount()).thenReturn(3);
		HitsplatApplied event = new HitsplatApplied();
		event.setActor(actor);
		event.setHitsplat(hitsplat);
		plugin.onHitsplatApplied(event);
	}

	private void gameState(GameState gameState)
	{
		GameStateChanged event = new GameStateChanged();
		event.setGameState(gameState);
		plugin.onGameStateChanged(event);
	}
}
