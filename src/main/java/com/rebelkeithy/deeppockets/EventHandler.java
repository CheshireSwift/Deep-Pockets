package com.rebelkeithy.deeppockets;

import com.rebelkeithy.deeppockets.item.ItemMiningPack;
import com.rebelkeithy.deeppockets.item.ItemMiningPack.StoredOre;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.util.SoundCategory;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.Random;

@Mod.EventBusSubscriber(modid = DeepPockets.MODID)
public class EventHandler {

    @SubscribeEvent
    public static void pickupItemEvent(EntityItemPickupEvent event) {
        if (event.getEntityPlayer() != null) {
            String oreName = DeepPocketsConfig.isOre(event.getItem().getItem());
            if (oreName != null) {
                EntityPlayer player = event.getEntityPlayer();
                for (ItemStack stack : player.inventory.mainInventory) {
                    if (stack.getItem() instanceof ItemMiningPack) {
                        ItemMiningPack pack = (ItemMiningPack) stack.getItem();

                        if (pack.numStoredOres(stack) < pack.getType().getSlots() || pack.containsOre(stack, oreName)) {
                            StoredOre ore = pack.getOre(stack, oreName);
                            ore.amount += event.getItem().getItem().getCount();
                            ore.displayName = event.getItem().getItem().getDisplayName();
                            if (ore.registryName == null) {
                                ore.registryName = event.getItem().getItem().getItem().getRegistryName().toString();
                                ore.meta = event.getItem().getItem().getItemDamage();
                            }
                            pack.setOre(stack, oreName, ore);

                            Random rand = new Random();
                            float f = ((rand.nextFloat() - rand.nextFloat()) * 0.7F + 1.0F) * 2.0F;
                            player.world.playSound(null, player.posX, player.posY, player.posZ, SoundEvents.ENTITY_ITEM_PICKUP, SoundCategory.AMBIENT, 0.2f, f);

                            event.getItem().setDead();
                            event.setCanceled(true);
                            return;
                        }
                    }
                }
            }
        }
    }
}
