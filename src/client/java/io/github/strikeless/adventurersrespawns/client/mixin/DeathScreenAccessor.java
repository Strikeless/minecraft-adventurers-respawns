package io.github.strikeless.adventurersrespawns.client.mixin;

import net.minecraft.client.gui.screen.DeathScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

@Mixin(DeathScreen.class)
public interface DeathScreenAccessor {
    @Accessor
    List<ButtonWidget> getButtons();

    @Accessor
    Text getScoreText();

    @Accessor
    void setScoreText(Text scoreText);
}
