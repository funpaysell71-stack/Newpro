import { world, system } from "@minecraft/server";
import { ActionFormData, ModalFormData } from "@minecraft/server-ui";

// Per-player runtime preferences. No modification of the Minecraft APK.
const prefs = new Map();
const defaults = () => ({ coords: true, clock: false, speed: false, nightVision: false, particles: false });
function getPrefs(player) {
  if (!prefs.has(player.id)) prefs.set(player.id, defaults());
  return prefs.get(player.id);
}
function fmt(n) { return Math.floor(n); }
function openMenu(player) {
  const p = getPrefs(player);
  const form = new ActionFormData()
    .title("§l§bNOVA §fBEDROCK")
    .body("§7Android utility client • Bedrock 26.52\n§8Choose a module")
    .button("§bHUD settings\n§7Coordinates • clock • speed")
    .button("§dVisual settings\n§7Night vision • particles")
    .button("§eAbout\n§7Compatibility and help");
  form.show(player).then(r => {
    if (r.canceled) return;
    if (r.selection === 0) openHud(player);
    if (r.selection === 1) openVisual(player);
    if (r.selection === 2) new ActionFormData().title("About NOVA").body("An independent Bedrock add-on inspired by customizable clients. This is not Flarial and does not modify Minecraft.\n\nOpen menu with /scriptevent nova:menu or use the compass item.").button("Back").show(player).then(() => openMenu(player));
  }).catch(() => {});
}
function openHud(player) {
  const p = getPrefs(player);
  new ModalFormData().title("§bHUD settings")
    .toggle("Show coordinates", { defaultValue: p.coords })
    .toggle("Show world time", { defaultValue: p.clock })
    .toggle("Show movement speed", { defaultValue: p.speed })
    .show(player).then(r => {
      if (r.canceled) return;
      [p.coords, p.clock, p.speed] = r.formValues;
      player.sendMessage("§aNOVA: HUD settings saved for this session.");
    }).catch(() => {});
}
function openVisual(player) {
  const p = getPrefs(player);
  new ModalFormData().title("§dVisual settings")
    .toggle("Night vision", { defaultValue: p.nightVision })
    .toggle("Footstep particles", { defaultValue: p.particles })
    .show(player).then(r => {
      if (r.canceled) return;
      [p.nightVision, p.particles] = r.formValues;
      player.sendMessage("§aNOVA: Visual settings saved for this session.");
    }).catch(() => {});
}
system.afterEvents.scriptEventReceive.subscribe(ev => {
  if (ev.id !== "nova:menu" || !ev.sourceEntity || ev.sourceEntity.typeId !== "minecraft:player") return;
  system.run(() => openMenu(ev.sourceEntity));
});
world.afterEvents.itemUse.subscribe(ev => {
  if (ev.itemStack?.typeId === "minecraft:compass") {
    system.run(() => openMenu(ev.source));
  }
});
let previous = new Map();
system.runInterval(() => {
  for (const player of world.getPlayers()) {
    const p = getPrefs(player);
    const loc = player.location;
    const parts = [];
    if (p.coords) parts.push("§bXYZ §f" + fmt(loc.x) + ", " + fmt(loc.y) + ", " + fmt(loc.z));
    if (p.clock) parts.push("§eTime §f" + world.getTimeOfDay());
    if (p.speed) {
      const last = previous.get(player.id);
      const distance = last ? Math.hypot(loc.x-last.x, loc.z-last.z) * 2 : 0;
      parts.push("§aSpeed §f" + distance.toFixed(1) + " b/s");
    }
    previous.set(player.id, { x: loc.x, z: loc.z });
    if (parts.length) player.onScreenDisplay.setActionBar(parts.join("  §8|  "));
    if (p.nightVision) {
      try { player.addEffect("night_vision", 80, { amplifier: 0, showParticles: false }); } catch {}
    }
    if (p.particles && Math.random() < 0.35) {
      try { player.dimension.spawnParticle("minecraft:basic_flame_particle", {x:loc.x,y:loc.y+0.1,z:loc.z}); } catch {}
    }
  }
}, 10);
world.afterEvents.playerLeave.subscribe(ev => {
  prefs.delete(ev.playerId);
  previous.delete(ev.playerId);
});
