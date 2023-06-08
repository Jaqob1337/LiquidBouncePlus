/*
 * LiquidBounce++ Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/PlusPlusMC/LiquidBouncePlusPlus/
 */
package net.ccbluex.liquidbounce.ui.client

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.ui.client.altmanager.GuiAltManager
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.ClientUtils
import net.ccbluex.liquidbounce.utils.misc.MiscUtils
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.minecraft.client.gui.*
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.renderer.texture.DynamicTexture
import net.minecraft.util.ResourceLocation
import net.minecraftforge.fml.client.GuiModList
import java.awt.Color
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.file.Files
import javax.imageio.ImageIO
import org.lwjgl.opengl.GL11
import kotlin.concurrent.thread
import kotlin.math.sin

class GuiMainMenu : GuiScreen(), GuiYesNoCallback {

    val bigLogo = ResourceLocation("liquidbounce+/big.png")
    val darkIcon = ResourceLocation("liquidbounce+/menu/dark.png")
    val lightIcon = ResourceLocation("liquidbounce+/menu/light.png")

    var slideX : Float = 0F
    var fade : Float = 0F

    var sliderX : Float = 0F
    var sliderDarkX : Float = 0F

    var lastAnimTick: Long = 0L
    var alrUpdate = false

    var lastXPos = 0F

    var extendedModMode = false
    var extendedBackgroundMode = false

    companion object {
        var useParallax = true
    }

    override fun initGui() {
        slideX = 0F
        fade = 0F
        sliderX = 0F
        sliderDarkX = 0F
        super.initGui()
    }

    override fun drawScreen(mouseX: Int, mouseY: Int, partialTicks: Float) {
        if (!alrUpdate) {
            renderParticlesSwitchButton()
            lastAnimTick = System.currentTimeMillis()
            alrUpdate = true
        }
        val creditInfo = "Copyright Mojang AB. Do not distribute!"
        drawBackground(0)
        GL11.glPushMatrix()
        renderSwitchButton()
        renderDarkModeButton()
        Fonts.font40.drawStringWithShadow("Paradox b${LiquidBounce.CLIENT_VERSION} | https://discord.gg/QCnuFpWA", 2F, height - 12F, -1)
        Fonts.font40.drawStringWithShadow(creditInfo, width - 3F - Fonts.font40.getStringWidth(creditInfo), height - 12F, -1)
        if (useParallax) moveMouseEffect(mouseX, mouseY, 10F)
        GlStateManager.disableAlpha()
        RenderUtils.drawImage2(bigLogo, width / 2F - 100F, height / 2F - 180F, 200, 200)
        GlStateManager.enableAlpha()
        renderBar(mouseX, mouseY, partialTicks)
        GL11.glPopMatrix()
        super.drawScreen(mouseX, mouseY, partialTicks)

        if (!LiquidBounce.mainMenuPrep) {
            val animProgress = ((System.currentTimeMillis() - lastAnimTick).toFloat() / 1500F).coerceIn(0F, 1F)
            RenderUtils.drawRect(0F, 0F, width.toFloat(), height.toFloat(), Color(0F, 0F, 0F, 1F - animProgress))
            if (animProgress >= 1F)
                LiquidBounce.mainMenuPrep = true
        }
    }

    override fun mouseClicked(mouseX: Int, mouseY: Int, mouseButton: Int) {
        if (!LiquidBounce.mainMenuPrep || mouseButton != 0) return

        if (isMouseHover(2F, height - 26F, 28F, height - 16F, mouseX, mouseY))
            useParallax = !useParallax

        if (isMouseHover(2F, height - 38F, 28F, height - 28F, mouseX, mouseY))
            GuiBackground.particles = !GuiBackground.particles

        if (isMouseHover(2F, height - 50F, 28F, height - 40F, mouseX, mouseY))
            GuiBackground.particles = !GuiBackground.particles

        val staticX = width / 2F - 120F
        val staticY = height / 2F + 20F
        var index: Int = 0
        for (icon in if (extendedModMode) ExtendedImageButton.values() else ImageButton.values()) {
            if (isMouseHover(staticX + 40F * index, staticY, staticX + 40F * (index + 1), staticY + 20F, mouseX, mouseY))
                when (index) {
                    0 -> if (extendedBackgroundMode) extendedBackgroundMode = false else if (extendedModMode) extendedModMode = false else mc.displayGuiScreen(GuiSelectWorld(this))
                    1 -> if (extendedBackgroundMode) GuiBackground.enabled = !GuiBackground.enabled else if (extendedModMode) mc.displayGuiScreen(GuiModList(this)) else mc.displayGuiScreen(GuiMultiplayer(this))
                    2 -> if (extendedBackgroundMode) GuiBackground.particles = !GuiBackground.particles else if (extendedModMode) mc.displayGuiScreen(GuiScripts(this)) else mc.displayGuiScreen(GuiAltManager(this))
                    3 -> if (extendedBackgroundMode) {
                        val file = MiscUtils.openFileChooser() ?: return
                        if (file.isDirectory) return

                        try {
                            Files.copy(file.toPath(), FileOutputStream(LiquidBounce.fileManager.backgroundFile))

                            val image = ImageIO.read(FileInputStream(LiquidBounce.fileManager.backgroundFile))
                            LiquidBounce.background = ResourceLocation("liquidbounce+/background.png")
                            mc.textureManager.loadTexture(LiquidBounce.background, DynamicTexture(image))
                        } catch (e: Exception) {
                            e.printStackTrace()
                            MiscUtils.showErrorPopup("Error", "Exception class: " + e.javaClass.name + "\nMessage: " + e.message)
                            LiquidBounce.fileManager.backgroundFile.delete()
                        }
                    } else if (extendedModMode) {
                            val rpc = LiquidBounce.clientRichPresence
                            rpc.showRichPresenceValue = when (val state = !rpc.showRichPresenceValue) {
                                false -> {
                                    rpc.shutdown()
                                    false
                                }
                                true -> {
                                    var value = true
                                    thread {
                                        value = try {
                                            rpc.setup()
                                            true
                                        } catch (throwable: Throwable) {
                                            ClientUtils.getLogger().error("Failed to setup Discord RPC.", throwable)
                                            false
                                        }
                                    }
                                    value
                                }
                            }
                        } else mc.displayGuiScreen(GuiOptions(this, this.mc.gameSettings))
                    4 -> if (extendedBackgroundMode) {
                        LiquidBounce.background = null
                        LiquidBounce.fileManager.backgroundFile.delete()
                    } else if (extendedModMode) extendedBackgroundMode = true else extendedModMode = true
                    5 -> mc.shutdown()
                }

            index++
        }

        super.mouseClicked(mouseX, mouseY, mouseButton)
    }

    fun moveMouseEffect(mouseX: Int, mouseY: Int, strength: Float) {
        val mX = mouseX - width / 2
        val mY = mouseY - height / 2
        val xDelta = mX.toFloat() / (width / 2).toFloat()
        val yDelta = mY.toFloat() / (height / 2).toFloat()

        GL11.glTranslatef(xDelta * strength, yDelta * strength, 0F)
    }

    fun renderSwitchButton() {
        sliderX = (sliderX + (if (useParallax) 2F else -2F)).coerceIn(0F, 12F)
        Fonts.font40.drawStringWithShadow("Parallax", 28F, height - 25F, -1)
        RenderUtils.drawRoundedRect(4F, height - 24F, 22F, height - 18F, 3F, if (useParallax) Color(0, 111, 255, 255).rgb else (if (LiquidBounce.darkMode) Color(70, 70, 70, 255) else Color(140, 140, 140, 255)).rgb)
        RenderUtils.drawRoundedRect(2F + sliderX, height - 26F, 12F + sliderX, height - 16F, 5F, Color.white.rgb)
    }

    fun renderDarkModeButton() {
        sliderDarkX = (sliderDarkX + (if (GuiBackground.particles) 2F else -2F)).coerceIn(0F, 12F)
        Fonts.font40.drawStringWithShadow("Particles", 28F, height - 37F, 0)
        RenderUtils.drawRoundedRect(4F, height - 36F, 22F, height - 30F, 3F, (if (GuiBackground.particles) Color(70, 70, 70, 255) else Color(140, 140, 140, 255)).rgb)
        RenderUtils.drawRoundedRect(2F + sliderDarkX, height - 38F, 12F + sliderDarkX, height - 28F, 5F, Color.white.rgb)
    }

    var sliderParticlesX : Float = 0F

    fun renderParticlesSwitchButton() {
        sliderParticlesX = (sliderParticlesX + (if (GuiBackground.particles) 2F else -2F)).coerceIn(0F, 12F)
        Fonts.font40.drawStringWithShadow("Particles", 28F, height - 50F, -1)
        RenderUtils.drawRoundedRect(4F, height - 48F, 22F, height - 42F, 3F, if (GuiBackground.particles) Color(0, 111, 255, 255).rgb else (if (LiquidBounce.darkMode) Color(70, 70, 70, 255) else Color(140, 140, 140, 255)).rgb)
        RenderUtils.drawRoundedRect(2F + sliderParticlesX, height - 50F, 12F + sliderParticlesX, height - 40F, 5F, Color.white.rgb)
    }

    fun renderBar(mouseX: Int, mouseY: Int, partialTicks: Float) {
        val staticX = width / 2F - 120F
        val staticY = height / 2F + 20F

        RenderUtils.drawRect(
            staticX.toDouble(),
            staticY.toDouble(),
            (staticX + 240F).toDouble(),
            (staticY + 20F).toDouble(),
            if (LiquidBounce.darkMode) Color(0, 0, 0, 0).rgb else Color(255, 255, 255, 0).rgb
        )

        var index = 0
        var shouldAnimate = false
        var displayString: String? = null
        var moveX = 0F
        val iconSize = 22F

        if (extendedModMode) {
            if (extendedBackgroundMode) {
                for (icon in ExtendedBackgroundButton.values()) {
                    val iconX = staticX + 40F * index + 11F
                    val iconY = staticY + 1F
                    val isMouseOver = isMouseHover(iconX, iconY, iconX + iconSize, iconY + iconSize, mouseX, mouseY)

                    val scaleFactor =
                        if (isMouseOver) 1F + sin(System.currentTimeMillis() / 500.0).toFloat() * 0.1F else 1F // Smooth animation using sine function only when hovering

                    if (isMouseOver) {
                        shouldAnimate = true
                        displayString = when (icon) {
                            ExtendedBackgroundButton.Enabled -> "Custom background: ${if (GuiBackground.enabled) "§aON" else "§cOFF"}"
                            ExtendedBackgroundButton.Particles -> "${icon.buttonName}: ${if (GuiBackground.particles) "§aON" else "§cOFF"}"
                            else -> icon.buttonName
                        }
                        moveX = staticX + 40F * index
                    }

                    GlStateManager.pushMatrix()
                    GlStateManager.translate(iconX, iconY, 0F)
                    GlStateManager.scale(scaleFactor, scaleFactor, 1F)
                    GlStateManager.translate(-iconX, -iconY, 0F)
                    RenderUtils.drawImage2(icon.texture, iconX, iconY, iconSize.toInt(), iconSize.toInt())
                    GlStateManager.popMatrix()

                    index++
                }
            } else {
                for (icon in ExtendedImageButton.values()) {
                    val iconX = staticX + 40F * index + 11F
                    val iconY = staticY + 1F
                    val isMouseOver = isMouseHover(iconX, iconY, iconX + iconSize, iconY + iconSize, mouseX, mouseY)

                    val scaleFactor =
                        if (isMouseOver) 1F + sin(System.currentTimeMillis() / 700.0).toFloat() * 0.2F else 1F // Smooth animation using sine function only when hovering

                    if (isMouseOver) {
                        shouldAnimate = true
                        displayString =
                            if (icon == ExtendedImageButton.DiscordRPC) "${icon.buttonName}: ${if (LiquidBounce.clientRichPresence.showRichPresenceValue) "§aON" else "§cOFF"}" else icon.buttonName
                        moveX = staticX + 40F * index
                    }

                    GlStateManager.pushMatrix()
                    GlStateManager.translate(iconX, iconY, 0F)
                    GlStateManager.scale(scaleFactor, scaleFactor, 1.2F)
                    GlStateManager.translate(-iconX, -iconY, 0F)
                    RenderUtils.drawImage2(icon.texture, iconX, iconY, iconSize.toInt(), iconSize.toInt())
                    GlStateManager.popMatrix()

                    index++
                }
            }
        } else {
            for (i in ImageButton.values()) {
                val iconX = staticX + 40F * index + 11F
                val iconY = staticY + 1F
                val isMouseOver = isMouseHover(iconX, iconY, iconX + iconSize, iconY + iconSize, mouseX, mouseY)

                val scaleFactor =
                    if (isMouseOver) 1.04F + sin(System.currentTimeMillis() / 6000.0).toFloat() * 0.1F else 1.06F // Smooth animation using sine function only when hovering

                if (isMouseOver) {
                    shouldAnimate = true
                    displayString = i.buttonName
                    moveX = staticX + 0F * index
                }

                GlStateManager.pushMatrix()
                GlStateManager.translate(iconX, iconY, 0F)
                GlStateManager.scale(scaleFactor, scaleFactor, 1F)
                GlStateManager.translate(-iconX, -iconY, 0F)
                RenderUtils.drawImage2(i.texture, iconX, iconY, iconSize.toInt(), iconSize.toInt())
                GlStateManager.popMatrix()

                index++
            }
        }

    }


    fun isMouseHover(x: Float, y: Float, x2: Float, y2: Float, mouseX: Int, mouseY: Int): Boolean = mouseX >= x && mouseX < x2 && mouseY >= y && mouseY < y2

    enum class ImageButton(val buttonName: String, val texture: ResourceLocation) {
        Single("Singleplayer", ResourceLocation("liquidbounce+/menu/singleplayer.png")),
        Multi("Multiplayer", ResourceLocation("liquidbounce+/menu/multiplayer.png")),
        Alts("Alts", ResourceLocation("liquidbounce+/menu/alt.png")),
        Settings("Settings", ResourceLocation("liquidbounce+/menu/settings.png")),
        Mods("Mods/Customize", ResourceLocation("liquidbounce+/menu/mods.png")),
        Exit("Exit", ResourceLocation("liquidbounce+/menu/exit.png"))
    }

    enum class ExtendedImageButton(val buttonName: String, val texture: ResourceLocation) {
        Back("Back", ResourceLocation("liquidbounce+/clickgui/back.png")),
        Mods("Mods", ResourceLocation("liquidbounce+/menu/mods.png")),
        Scripts("Scripts", ResourceLocation("liquidbounce+/clickgui/docs.png")),
        DiscordRPC("Discord RPC", ResourceLocation("liquidbounce+/menu/discord.png")),
        Background("Background", ResourceLocation("liquidbounce+/menu/wallpaper.png")),
        Exit("Exit", ResourceLocation("liquidbounce+/menu/exit.png"))
    }

    enum class ExtendedBackgroundButton(val buttonName: String, val texture: ResourceLocation) {
        Back("Back", ResourceLocation("liquidbounce+/clickgui/back.png")),
        Enabled("Enabled", ResourceLocation("liquidbounce+/notification/new/checkmark.png")),
        Particles("Particles", ResourceLocation("liquidbounce+/clickgui/brush.png")),
        Change("Change wallpaper", ResourceLocation("liquidbounce+/clickgui/import.png")),
        Reset("Reset wallpaper", ResourceLocation("liquidbounce+/clickgui/reload.png")),
        Exit("Exit", ResourceLocation("liquidbounce+/menu/exit.png"))
    }

    fun isMouseOverIcon(mouseX: Int, mouseY: Int, iconX: Float, iconY: Float, iconWidth: Float, iconHeight: Float): Boolean {
        return mouseX in iconX.toInt()..(iconX + iconWidth).toInt() && mouseY in iconY.toInt()..(iconY + iconHeight).toInt()
    }

    override fun keyTyped(typedChar: Char, keyCode: Int) {}
}
