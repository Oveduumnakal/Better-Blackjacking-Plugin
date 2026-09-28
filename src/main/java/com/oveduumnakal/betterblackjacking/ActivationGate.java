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

import net.runelite.api.coords.WorldPoint;

/**
 * Decides whether the plugin is active and which targets it draws, from three plain values: where the
 * player is, what they wield and their boosted Thieving level.
 *
 * <p>The plugin is active only while the player is in {@link Pollnivneach} with a blackjack
 * ({@link Blackjacks}) in the weapon slot. A {@link BlackjackTarget} is eligible when the plugin is
 * active and the boosted Thieving level is at least the target's requirement.
 *
 * <p>A gate is immutable and cheap to build, so the plugin can keep one and replace it from event
 * handlers: {@link #withRegionId(int)} on movement, {@link #withWeaponItemId(int)} on
 * {@code ItemContainerChanged} and {@link #withThievingLevel(int)} on {@code StatChanged}.
 */
final class ActivationGate
{
	/** The item ID of an empty weapon slot. */
	static final int NO_WEAPON = -1;

	/** A closed gate: no region, no weapon, Thieving level 0. Use it before the player logs in. */
	static final ActivationGate CLOSED = new ActivationGate(-1, NO_WEAPON, 0);

	private final int regionId;

	private final int weaponItemId;

	private final int thievingLevel;

	private final boolean active;

	private ActivationGate(int regionId, int weaponItemId, int thievingLevel)
	{
		this.regionId = regionId;
		this.weaponItemId = weaponItemId;
		this.thievingLevel = thievingLevel;
		this.active = Pollnivneach.containsRegion(regionId) && Blackjacks.isBlackjack(weaponItemId);
	}

	/**
	 * Builds a gate from the player's map region.
	 *
	 * @param regionId the region the player is in, as from {@link WorldPoint#getRegionID()}
	 * @param weaponItemId the item ID in the weapon slot, or {@link #NO_WEAPON} if it's empty
	 * @param boostedThievingLevel the player's boosted Thieving level
	 * @return the gate
	 */
	static ActivationGate of(int regionId, int weaponItemId, int boostedThievingLevel)
	{
		return new ActivationGate(regionId, weaponItemId, boostedThievingLevel);
	}

	/**
	 * Builds a gate from the player's world location.
	 *
	 * @param location the player's world location; {@code null} counts as outside Pollnivneach
	 * @param weaponItemId the item ID in the weapon slot, or {@link #NO_WEAPON} if it's empty
	 * @param boostedThievingLevel the player's boosted Thieving level
	 * @return the gate
	 */
	static ActivationGate of(WorldPoint location, int weaponItemId, int boostedThievingLevel)
	{
		return of(regionOf(location), weaponItemId, boostedThievingLevel);
	}

	/**
	 * Returns a gate with the player in a different region, or this gate if the region is unchanged.
	 *
	 * @param newRegionId the region the player is now in
	 * @return the updated gate
	 */
	ActivationGate withRegionId(int newRegionId)
	{
		return newRegionId == regionId ? this : of(newRegionId, weaponItemId, thievingLevel);
	}

	/**
	 * Returns a gate with the player at a different location, or this gate if the region is unchanged.
	 *
	 * @param location the player's world location; {@code null} counts as outside Pollnivneach
	 * @return the updated gate
	 */
	ActivationGate withLocation(WorldPoint location)
	{
		return withRegionId(regionOf(location));
	}

	/**
	 * Returns a gate with a different weapon, or this gate if the weapon is unchanged.
	 *
	 * @param newWeaponItemId the item ID now in the weapon slot, or {@link #NO_WEAPON} if it's empty
	 * @return the updated gate
	 */
	ActivationGate withWeaponItemId(int newWeaponItemId)
	{
		return newWeaponItemId == weaponItemId ? this : of(regionId, newWeaponItemId, thievingLevel);
	}

	/**
	 * Returns a gate with a different boosted Thieving level, or this gate if the level is unchanged.
	 *
	 * @param newBoostedThievingLevel the player's boosted Thieving level
	 * @return the updated gate
	 */
	ActivationGate withThievingLevel(int newBoostedThievingLevel)
	{
		return newBoostedThievingLevel == thievingLevel ? this : of(regionId, weaponItemId, newBoostedThievingLevel);
	}

	/**
	 * Whether the plugin is active: the player is in Pollnivneach with a blackjack equipped.
	 *
	 * @return {@code true} if the plugin should track and draw targets
	 */
	boolean isActive()
	{
		return active;
	}

	/**
	 * Whether a target is eligible: the plugin is active and the boosted Thieving level meets the
	 * target's requirement.
	 *
	 * @param target the target; may be {@code null}
	 * @return {@code true} if the target should be tracked and drawn; {@code false} for {@code null}
	 */
	boolean isEligible(BlackjackTarget target)
	{
		return active && target != null && thievingLevel >= target.thievingLevel();
	}

	/**
	 * Whether an NPC is an eligible target, as {@link #isEligible(BlackjackTarget)} for the target
	 * its ID belongs to.
	 *
	 * @param npcId the NPC's ID, as from {@code NPC#getId()}
	 * @return {@code true} if the NPC is a blackjack target that is eligible
	 */
	boolean isEligibleNpc(int npcId)
	{
		return active && isEligible(BlackjackTarget.forNpcId(npcId));
	}

	/**
	 * Returns the region the gate was built with.
	 *
	 * @return the region ID
	 */
	int regionId()
	{
		return regionId;
	}

	/**
	 * Returns the weapon-slot item ID the gate was built with.
	 *
	 * @return the item ID, or {@link #NO_WEAPON}
	 */
	int weaponItemId()
	{
		return weaponItemId;
	}

	/**
	 * Returns the boosted Thieving level the gate was built with.
	 *
	 * @return the boosted Thieving level
	 */
	int thievingLevel()
	{
		return thievingLevel;
	}

	/**
	 * Describes the gate's inputs and result, for debug logging.
	 *
	 * @return a one-line description
	 */
	@Override
	public String toString()
	{
		return "ActivationGate{region=" + regionId + ", weapon=" + weaponItemId + ", thieving=" + thievingLevel
				+ ", active=" + active + "}";
	}

	private static int regionOf(WorldPoint location)
	{
		return location == null ? -1 : location.getRegionID();
	}
}
