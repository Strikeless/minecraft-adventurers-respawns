package io.github.strikeless.adventurersrespawns.client.mixin;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

@Mixin(DeathScreen.class)
public interface DeathScreenAccessor {
    @Accessor
    List<Button> getExitButtons();

    @Accessor
    Component getDeathScore();

    @Accessor
    void setDeathScore(Component scoreText);
}
