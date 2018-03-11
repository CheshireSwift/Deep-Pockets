package com.rebelkeithy.deeppockets.item;

import com.rebelkeithy.deeppockets.DeepPockets;
import com.rebelkeithy.deeppockets.DeepPocketsConfig;
import net.minecraft.client.renderer.ItemMeshDefinition;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@Mod.EventBusSubscriber(modid = DeepPockets.MODID)
@GameRegistry.ObjectHolder(DeepPockets.MODID)
public class RegistrarDeepPockets {

    public static final Item MINING_PACK = Items.AIR;
    public static final Item MINING_PACK_ADVANCED = Items.AIR;

    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) {
        DeepPocketsConfig.buildItemList();

        event.getRegistry().registerAll(
                new ItemMiningPack(PackTypes.BASIC).setRegistryName("mining_pack"),
                new ItemMiningPack(PackTypes.ADVANCED).setRegistryName("mining_pack_advanced")
        );
    }

    @SideOnly(Side.CLIENT)
    @SubscribeEvent
    public static void registerModels(ModelRegistryEvent event) {
        final ItemMeshDefinition packDefinition = stack -> {
            if (!(stack.getItem() instanceof ItemMiningPack))
                return new ModelResourceLocation("potato", "inventory"); // Shrug

            ItemMiningPack miningPack = (ItemMiningPack) stack.getItem();
            int stored = miningPack.getTotalItems(stack);
            int modelIndex = Math.min(1 + stored / 128, 4);
            return new ModelResourceLocation(stack.getItem().getRegistryName().toString() + "_" + modelIndex, "inventory");
        };

        for (int i = 1; i <= 4; i++) {
            ModelLoader.registerItemVariants(MINING_PACK, new ModelResourceLocation(MINING_PACK.getRegistryName().toString() + "_" + i, "inventory"));
            ModelLoader.registerItemVariants(MINING_PACK_ADVANCED, new ModelResourceLocation(MINING_PACK_ADVANCED.getRegistryName().toString() + "_" + i, "inventory"));
        }

        ModelLoader.setCustomMeshDefinition(MINING_PACK, packDefinition);
        ModelLoader.setCustomMeshDefinition(MINING_PACK_ADVANCED, packDefinition);
    }
}
