package com.fennek.carworks.content.blocks.steeringwheel.handlers;

import com.simibubi.create.Create;
import com.simibubi.create.content.redstone.link.IRedstoneLinkable;
import com.simibubi.create.content.redstone.link.LinkBehaviour;
import com.simibubi.create.content.redstone.link.RedstoneLinkNetworkHandler;
import com.simibubi.create.foundation.advancement.AllAdvancements;
import net.createmod.catnip.data.Couple;
import net.createmod.catnip.data.IntAttached;
import net.createmod.catnip.data.WorldAttached;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelAccessor;

import java.util.*;

public class SteeringWheelServerHandler {

    public static WorldAttached<Map<UUID, Collection<SteeringWheelServerHandler.ManualFrequencyEntry>>> receivedInputs =
            new WorldAttached<>($ -> new HashMap<>());
    static final int TIMEOUT = 30;

    public static void tick(LevelAccessor world) {
        Map<UUID, Collection<SteeringWheelServerHandler.ManualFrequencyEntry>> map = receivedInputs.get(world);
        for (Iterator<Map.Entry<UUID, Collection<SteeringWheelServerHandler.ManualFrequencyEntry>>> iterator = map.entrySet()
                .iterator(); iterator.hasNext(); ) {

            Map.Entry<UUID, Collection<SteeringWheelServerHandler.ManualFrequencyEntry>> entry = iterator.next();
            Collection<SteeringWheelServerHandler.ManualFrequencyEntry> list = entry.getValue();

            for (Iterator<SteeringWheelServerHandler.ManualFrequencyEntry> entryIterator = list.iterator(); entryIterator.hasNext(); ) {
                SteeringWheelServerHandler.ManualFrequencyEntry manualFrequencyEntry = entryIterator.next();
                manualFrequencyEntry.decrement();
                if (!manualFrequencyEntry.isAlive()) {
                    Create.REDSTONE_LINK_NETWORK_HANDLER.removeFromNetwork(world, manualFrequencyEntry);
                    entryIterator.remove();
                }
            }

            if (list.isEmpty())
                iterator.remove();
        }
    }

    public static void receivePressed(LevelAccessor world, BlockPos pos, UUID uniqueID, List<Couple<RedstoneLinkNetworkHandler.Frequency>> collect,
                                      boolean pressed) {
        Map<UUID, Collection<SteeringWheelServerHandler.ManualFrequencyEntry>> map = receivedInputs.get(world);
        Collection<SteeringWheelServerHandler.ManualFrequencyEntry> list = map.computeIfAbsent(uniqueID, $ -> new ArrayList<>());

        WithNext:
        for (Couple<RedstoneLinkNetworkHandler.Frequency> activated : collect) {
            for (SteeringWheelServerHandler.ManualFrequencyEntry entry : list) {
                if (entry.getSecond()
                        .equals(activated)) {
                    if (!pressed)
                        entry.setFirst(0);
                    else
                        entry.updatePosition(pos);
                    continue WithNext;
                }
            }

            if (!pressed)
                continue;

            SteeringWheelServerHandler.ManualFrequencyEntry entry = new SteeringWheelServerHandler.ManualFrequencyEntry(pos, activated);
            Create.REDSTONE_LINK_NETWORK_HANDLER.addToNetwork(world, entry);
            list.add(entry);

            for (IRedstoneLinkable linkable : Create.REDSTONE_LINK_NETWORK_HANDLER.getNetworkOf(world, entry))
                if (linkable instanceof LinkBehaviour lb && lb.isListening())
                    AllAdvancements.LINKED_CONTROLLER.awardTo(world.getPlayerByUUID(uniqueID));
        }
    }

    static class ManualFrequencyEntry extends IntAttached<Couple<RedstoneLinkNetworkHandler.Frequency>> implements IRedstoneLinkable {

        private BlockPos pos;

        public ManualFrequencyEntry(BlockPos pos, Couple<RedstoneLinkNetworkHandler.Frequency> second) {
            super(TIMEOUT, second);
            this.pos = pos;
        }

        public void updatePosition(BlockPos pos) {
            this.pos = pos;
            setFirst(TIMEOUT);
        }

        @Override
        public int getTransmittedStrength() {
            return isAlive() ? 15 : 0;
        }

        @Override
        public boolean isAlive() {
            return getFirst() > 0;
        }

        @Override
        public BlockPos getLocation() {
            return pos;
        }

        @Override
        public void setReceivedStrength(int power) {
        }

        @Override
        public boolean isListening() {
            return false;
        }

        @Override
        public Couple<RedstoneLinkNetworkHandler.Frequency> getNetworkKey() {
            return getSecond();
        }

    }

}