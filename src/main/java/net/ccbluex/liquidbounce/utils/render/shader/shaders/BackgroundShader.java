/*
 * LiquidBounce++ Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/PlusPlusMC/LiquidBouncePlusPlus/
 */
package net.ccbluex.liquidbounce.utils.render.shader.shaders;

import net.ccbluex.liquidbounce.utils.render.RenderUtils;
import net.ccbluex.liquidbounce.utils.render.shader.Shader;
import net.minecraft.client.gui.ScaledResolution;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.Display;

public final class BackgroundShader extends Shader {

    public final static BackgroundShader BACKGROUND_SHADER = new BackgroundShader();

    private float time;

    public BackgroundShader() {
        super("background.frag");
    }

    @Override
    public void setupUniforms() {
        setupUniform("resolution");
        setupUniform("time");
    }



    @Override
    public void updateUniforms() {
        final ScaledResolution scaledResolution = new ScaledResolution(mc);

        final int resolutionID = getUniform("resolution");
        if(resolutionID > -1)
            GL20.glUniform2f(resolutionID, (float) scaledResolution.getScaledWidth(), (float) scaledResolution.getScaledHeight());
        final int timeID = getUniform("time");
        if(timeID > -1) GL20.glUniform1f(timeID, time);

        time += 0.005F * RenderUtils.deltaTime;
    }


}
