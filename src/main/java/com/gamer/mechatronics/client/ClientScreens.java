package com.gamer.mechatronics.client;

import com.gamer.mechatronics.Mechatronics;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@EventBusSubscriber(modid = Mechatronics.MODID, value = Dist.CLIENT)
public class ClientScreens {
    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(Mechatronics.BREADBOARD_MENU.get(), BreadboardScreen::new);
    }
}