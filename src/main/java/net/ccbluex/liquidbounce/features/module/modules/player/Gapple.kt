/*
 * LiquidBounce++ Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/PlusPlusMC/LiquidBouncePlusPlus/
 */
package net.ccbluex.liquidbounce.features.module.modules.player

import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.event.WorldEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.utils.InventoryUtils
import net.ccbluex.liquidbounce.utils.timer.MSTimer
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.FloatValue
import net.ccbluex.liquidbounce.value.IntegerValue
import net.ccbluex.liquidbounce.value.ListValue
import net.minecraft.client.gui.inventory.GuiContainer
import net.minecraft.init.Items
import net.minecraft.network.play.client.C03PacketPlayer
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement
import net.minecraft.network.play.client.C09PacketHeldItemChange
import net.minecraft.network.play.server.S09PacketHeldItemChange
import net.minecraft.potion.Potion
import net.minecraft.util.MathHelper
import java.util.*

@ModuleInfo(name = "Gapple", description = "Eat Gapples.", category = ModuleCategory.PLAYER)
class Gapple : Module() {

    private val modeValue = ListValue("Mode", arrayOf("Auto", "LegitAuto", "Legit", "Head"), "Auto")
    private val percent = FloatValue("HealthPercent", 75.0f, 1.0f, 100.0f)
    private val min = IntegerValue("MinDelay", 2000, 1, 5000)
    private val max = IntegerValue("MaxDelay", 100, 1, 5000)
    private val regenSec = FloatValue("MinRegenSec", 4.6f, 0.0f, 10.0f)
    private val groundCheck = BoolValue("OnlyOnGround", false)
    private val waitRegen = BoolValue("WaitRegen", true)
    private val invCheck = BoolValue("InvCheck", false)
    private val absorpCheck = BoolValue("NoAbsorption", true)
    private val fastEatValue = BoolValue("FastEat", false)
    private val eatDelayValue = IntegerValue("FastEatDelay", 14, 0, 35)
    private val timer = MSTimer()
    private var eating = -1
    private var delay = 0
    private var isDisable = false
    private var tryHeal = false
    private var prevSlot = -1
    private var switchBack = false
    private var lastHealTime: Long = 0

    override fun onEnable() {
        eating = -1
        prevSlot = -1
        switchBack = false
        timer.reset()
        isDisable = false
        tryHeal = false
        delay = MathHelper.getRandomIntegerInRange(Random(), min.get(), max.get())
        lastHealTime = System.currentTimeMillis()
    }

    @EventTarget
    fun onWorld(event: WorldEvent) {
        isDisable = true
        tryHeal = false
    }

    @EventTarget
    fun onPacket(event: PacketEvent) {
        if (eating != -1 && event.packet is C03PacketPlayer) {
            eating++
        } else if (event.packet is S09PacketHeldItemChange || event.packet is C09PacketHeldItemChange) {
            eating = -1
        }
    }

    @EventTarget
    fun onUpdate(event: UpdateEvent) {
        if (tryHeal) {
            val currentTime = System.currentTimeMillis()
            val elapsed = currentTime - lastHealTime

            if (elapsed >= delay) {
                val mode: String = modeValue.get().lowercase(Locale.getDefault())
                when (mode) {
                    "auto" -> {
                        val gappleInHotbar = InventoryUtils.findItem(36, 45, Items.golden_apple)
                        if (gappleInHotbar != -1) {
                            mc.netHandler.addToSendQueue(C09PacketHeldItemChange(gappleInHotbar - 36))
                            mc.netHandler.addToSendQueue(C08PacketPlayerBlockPlacement(mc.thePlayer.heldItem))
                            repeat(35) {
                                mc.netHandler.addToSendQueue(C03PacketPlayer(mc.thePlayer.onGround))
                            }
                            mc.netHandler.addToSendQueue(C09PacketHeldItemChange(mc.thePlayer.inventory.currentItem))
                            tryHeal = false
                            timer.reset()
                            delay = MathHelper.getRandomIntegerInRange(Random(), min.get(), max.get())
                            lastHealTime = System.currentTimeMillis()
                        } else {
                            tryHeal = false
                        }
                    }

                    "legitauto" -> {
                        if (eating == -1) {
                            val gappleInHotbar = InventoryUtils.findItem(36, 45, Items.golden_apple)
                            if (gappleInHotbar == -1) {
                                tryHeal = false
                                return
                            }
                            mc.netHandler.addToSendQueue(C09PacketHeldItemChange(gappleInHotbar - 36))
                            mc.netHandler.addToSendQueue(C08PacketPlayerBlockPlacement(mc.thePlayer.heldItem))
                            eating = 0
                        } else if (eating > 35) {
                            repeat(35 - eating) {
                                mc.netHandler.addToSendQueue(C03PacketPlayer(mc.thePlayer.onGround))
                            }
                            mc.netHandler.addToSendQueue(C09PacketHeldItemChange(mc.thePlayer.inventory.currentItem))
                            timer.reset()
                            tryHeal = false
                            delay = MathHelper.getRandomIntegerInRange(
                                Random(),
                                min.get(),
                                max.get()
                            ) // Verzögerungswert direkt ändern
                            lastHealTime = System.currentTimeMillis()
                        }
                    }

                    "legit" -> {
                        if (eating == -1) {
                            val gappleInHotbar = InventoryUtils.findItem(36, 45, Items.golden_apple)
                            if (gappleInHotbar == -1) {
                                tryHeal = false
                                return
                            }
                            if (prevSlot == -1) {
                                prevSlot = mc.thePlayer.inventory.currentItem
                            }
                            mc.thePlayer.inventory.currentItem = gappleInHotbar - 36
                            mc.netHandler.addToSendQueue(C08PacketPlayerBlockPlacement(mc.thePlayer.heldItem))
                            eating = 0
                        } else if (eating > 35 || (fastEatValue.get() && eating > eatDelayValue.get())) {
                            repeat(35 - eating) {
                                mc.netHandler.addToSendQueue(C03PacketPlayer(mc.thePlayer.onGround))
                            }
                            timer.reset()
                            tryHeal = false
                            delay = MathHelper.getRandomIntegerInRange(Random(), min.get(), max.get())
                            lastHealTime = System.currentTimeMillis()
                        }
                    }

                    "head" -> {
                        val headInHotbar = InventoryUtils.findItem(36, 45, Items.skull)
                        if (headInHotbar != -1) {
                            mc.netHandler.addToSendQueue(C09PacketHeldItemChange(headInHotbar - 36))
                            mc.netHandler.addToSendQueue(C08PacketPlayerBlockPlacement(mc.thePlayer.heldItem))
                            mc.netHandler.addToSendQueue(C09PacketHeldItemChange(mc.thePlayer.inventory.currentItem))
                            timer.reset()
                            tryHeal = false
                            delay = MathHelper.getRandomIntegerInRange(Random(), min.get(), max.get())
                            lastHealTime = System.currentTimeMillis()
                        } else {
                            tryHeal = false
                        }
                    }
                }
            }
        }

        if (mc.thePlayer.ticksExisted <= 10 && isDisable) {
            isDisable = false
        }

        val absorp = MathHelper.ceiling_double_int(mc.thePlayer.absorptionAmount.toDouble())

        if (!tryHeal && prevSlot != -1) {
            if (!switchBack) {
                switchBack = true
                return
            }
            mc.thePlayer.inventory.currentItem = prevSlot
            eating = -1
            prevSlot = -1
            switchBack = false
        }

        if ((groundCheck.get() && !mc.thePlayer.onGround) || (invCheck.get() && mc.currentScreen is GuiContainer) || (absorp > 0 && absorpCheck.get())) {
            return
        }

        if (waitRegen.get() && mc.thePlayer.isPotionActive(Potion.regeneration) && mc.thePlayer.getActivePotionEffect(Potion.regeneration).duration > regenSec.get() * 20.0f) {
            return
        }

        if (!isDisable && (mc.thePlayer.health <= (percent.get() / 100.0f) * mc.thePlayer.maxHealth) && timer.hasTimePassed(delay.toLong())) {
            if (tryHeal) {
                return
            }
            tryHeal = true
        }
    }

    override val tag: String
        get() = modeValue.get()
}