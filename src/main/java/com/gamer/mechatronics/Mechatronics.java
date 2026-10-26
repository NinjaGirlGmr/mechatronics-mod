package com.gamer.mechatronics;

import com.gamer.mechatronics.block.*;
import com.gamer.mechatronics.item.LedItem;
import com.gamer.mechatronics.item.SwitchItem;
import com.gamer.mechatronics.menu.BreadboardMenu;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.Supplier;


// The value here should match an entry in the META-INF/neoforge.mods.toml file
@Mod(Mechatronics.MODID)
public class Mechatronics {
    // Define mod id in a common place for everything to reference
    public static final String MODID = "mechatronics";
    // Directly reference a slf4j logger
    public static final Logger LOGGER = LogUtils.getLogger();
    // Create a Deferred Register to hold Blocks which will all be registered under the "mechatronics" namespace
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MODID);
    // Create a Deferred Register to hold Items which will all be registered under the "mechatronics" namespace
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);
    // Create a Deferred Register to hold CreativeModeTabs which will all be registered under the "mechatronics" namespace
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    // Creates a new Block with the id "mechatronics:example_block", combining the namespace and path
    public static final DeferredBlock<Block> EXAMPLE_BLOCK = BLOCKS.registerSimpleBlock("example_block", p -> p.mapColor(MapColor.STONE));
    // Creates a new BlockItem with the id "mechatronics:example_block", combining the namespace and path
    public static final DeferredItem<BlockItem> EXAMPLE_BLOCK_ITEM = ITEMS.registerSimpleBlockItem("example_block", EXAMPLE_BLOCK);


    /*
    * The above is the template information, the direct section bellow is where the items are now being declared.
    *
    */

    // Deffered Register
    public static final DeferredItem<Item> COPPER_WIRE = ITEMS.registerSimpleItem("copper_wire");
    public static final DeferredItem<Item> RESISTOR =ITEMS.registerSimpleItem("resistor");
    public static final DeferredItem<SwitchItem> TOGGLE_SWITCH = ITEMS.registerItem("toggle_switch", SwitchItem::new);
    // helps set up screen display for bread board
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, MODID);
    //bread board (physical)
    // bread board sizes as blocks
    public static final DeferredBlock<BreadboardBlock> SMALL_BREADBOARD = registerBreadboard("small_breadboard", 15);
    public static final DeferredBlock<BreadboardBlock> MEDIUM_BREADBOARD = registerBreadboard("medium_breadboard", 30);
    public static final DeferredBlock<BreadboardBlock> LARGE_BREADBOARD = registerBreadboard("large_breadboard", 60);
    // bread board sizes as items
    public static final DeferredItem<BlockItem> SMALL_BREADBOARD_ITEM = ITEMS.registerSimpleBlockItem("small_breadboard", SMALL_BREADBOARD);
    public static final DeferredItem<BlockItem> MEDIUM_BREADBOARD_ITEM = ITEMS.registerSimpleBlockItem("medium_breadboard", MEDIUM_BREADBOARD);
    public static final DeferredItem<BlockItem> LARGE_BREADBOARD_ITEM = ITEMS.registerSimpleBlockItem("large_breadboard", LARGE_BREADBOARD);

    // All breadboards share the same properties; only the column count differs
    private static DeferredBlock<BreadboardBlock> registerBreadboard(String name, int columns) {
        return BLOCKS.registerBlock(name,
                p -> new BreadboardBlock(p, columns),
                p -> p.mapColor(MapColor.WOOD).strength(0.5f).noOcclusion());
    }

    // bread board entity
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, MODID);

    //Led Color Items
    public static final Map<DyeColor, DeferredItem<LedItem>> LEDS = new EnumMap<>(DyeColor.class);
    static {
        for (DyeColor color : DyeColor.values()) {
            LEDS.put(color, ITEMS.registerItem(color.getName() + "_led",
                    p -> new LedItem(p, ledVoltage(color))));
        }
    }

    // Roughly real forward voltages: red needs the least, blue and white the most
    private static double ledVoltage(DyeColor color) {
        return switch (color) {
            case RED, BROWN -> 1.8;
            case ORANGE -> 2.0;
            case YELLOW, LIME -> 2.1;
            case GREEN -> 2.2;
            default -> 3.0;
        };
    }

    //BREAD BOARD NIGHTMARE
    // bread board entity supplier
    public static final Supplier<BlockEntityType<BreadboardBlockEntity>> BREADBOARD_BE = BLOCK_ENTITY_TYPES.register("breadboard", () -> new BlockEntityType<>(BreadboardBlockEntity::new, false, SMALL_BREADBOARD.get(), MEDIUM_BREADBOARD.get(), LARGE_BREADBOARD.get()));
    // sets up the screen for bread board entity menu
    public static final Supplier<MenuType<BreadboardMenu>> BREADBOARD_MENU =
            MENUS.register("breadboard", () -> IMenuTypeExtension.create(BreadboardMenu::new));


    //CREATIVE
    // Creates a creative tab with the id "mechatronics:example_tab" for the example item, that is placed after the combat tab
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MECHATRONICS_TAB = CREATIVE_MODE_TABS.register("example_tab", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.mechatronics")) //Name of tab
            .withTabsBefore(CreativeModeTabs.COMBAT)
            .icon(() -> SMALL_BREADBOARD_ITEM.get().getDefaultInstance())
            .displayItems((parameters, output) -> {
                output.accept(COPPER_WIRE.get()); // Adds in the copper wire to creative
                output.accept(RESISTOR.get()); // Adds in the Resistor to creative
                output.accept(SMALL_BREADBOARD_ITEM.get());  // breadboard (small)
                output.accept(MEDIUM_BREADBOARD_ITEM.get()); // breadboard (medium)
                output.accept(LARGE_BREADBOARD_ITEM.get());  // breadboard (large)
                LEDS.values().forEach(led -> output.accept(led.get())); // add multicolor leds
                output.accept(TOGGLE_SWITCH.get());// gets toggle switch for circuits.
            }).build());

    // The constructor for the mod class is the first code that is run when your mod is loaded.
    // FML will recognize some parameter types like IEventBus or ModContainer and pass them in automatically.
    public Mechatronics(IEventBus modEventBus, ModContainer modContainer) {
        // Register the commonSetup method for modloading
        modEventBus.addListener(this::commonSetup);

        // Register the Deferred Register to the mod event bus so blocks get registered
        BLOCKS.register(modEventBus);
        // Register the Deferred Register to the mod event bus so items get registered
        ITEMS.register(modEventBus);
        // Register the Deferred Register to the mod event bus so tabs get registered
        CREATIVE_MODE_TABS.register(modEventBus);

        // bread board ui interegration
        BLOCK_ENTITY_TYPES.register(modEventBus);
        MENUS.register(modEventBus);

        // Register ourselves for server and other game events we are interested in.
        // Note that this is necessary if and only if we want *this* class (Mechatronics) to respond directly to events.
        // Do not add this line if there are no @SubscribeEvent-annotated functions in this class, like onServerStarting() below.
        NeoForge.EVENT_BUS.register(this);

        // Register the item to a creative tab
        modEventBus.addListener(this::addCreative);

        // Register our mod's ModConfigSpec so that FML can create and load the config file for us
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        // Some common setup code
        LOGGER.info("HELLO FROM COMMON SETUP");

        if (Config.LOG_DIRT_BLOCK.getAsBoolean()) {
            LOGGER.info("DIRT BLOCK >> {}", BuiltInRegistries.BLOCK.getKey(Blocks.DIRT));
        }

        LOGGER.info("{}{}", Config.MAGIC_NUMBER_INTRODUCTION.get(), Config.MAGIC_NUMBER.getAsInt());

        Config.ITEM_STRINGS.get().forEach((item) -> LOGGER.info("ITEM >> {}", item));
    }

    // Add the example block item to the building blocks tab
    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.BUILDING_BLOCKS) {
            event.accept(EXAMPLE_BLOCK_ITEM);
        }
    }

    // You can use SubscribeEvent and let the Event Bus discover methods to call
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        // Do something when the server starts
        LOGGER.info("HELLO from server starting");
    }
}
