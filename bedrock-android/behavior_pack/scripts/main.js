import { world, system } from '@minecraft/server';
import { ModalFormData } from '@minecraft/server-ui';
import { decodePrefs, hudText } from './core.js';

const PROPERTY = 'nevertime:preferences_v1';
const cache = new Map();
const openForms = new Set();
const activeBars = new Set();
function preferences(player) {
  if (!cache.has(player.id)) cache.set(player.id, decodePrefs(player.getDynamicProperty(PROPERTY)));
  return cache.get(player.id);
}
async function openMenu(player) {
  const id = player.id;
  if (openForms.has(id)) return;
  openForms.add(id);
  try {
    const p = preferences(player);
    const result = await new ModalFormData().title('§bNevertime §f• HUD')
      .toggle('HUD включён', { defaultValue: p.enabled })
      .toggle('Координаты XYZ', { defaultValue: p.coords })
      .toggle('Время мира', { defaultValue: p.clock })
      .toggle('Скорость по горизонтали (блоков/тик)', { defaultValue: p.speed })
      .toggle('Направление S / W / N / E', { defaultValue: p.direction })
      .toggle('Измерение', { defaultValue: p.dimension }).submitButton('Сохранить').show(player);
    if (result.canceled) {
      if (result.cancelationReason === 'UserBusy') player.sendMessage('§bNevertime: §fзакрой чат и снова используй компас.');
      return;
    }
    const keys = ['enabled', 'coords', 'clock', 'speed', 'direction', 'dimension'];
    if (!result.formValues || result.formValues.length !== keys.length || result.formValues.some(v => typeof v !== 'boolean')) return;
    const next = Object.fromEntries(keys.map((key, i) => [key, result.formValues[i]]));
    player.setDynamicProperty(PROPERTY, JSON.stringify(next));
    cache.set(id, next);
    player.sendMessage('§bNevertime: §fнастройки сохранены в этом мире.');
  } catch (error) {
    console.warn(`Nevertime menu: ${error}`);
    try { player.sendMessage('§cNevertime: не удалось открыть меню или сохранить настройки. Попробуй ещё раз.'); } catch { /* Player disconnected. */ }
  } finally { openForms.delete(id); }
}
system.afterEvents.scriptEventReceive.subscribe(event => {
  if (event.id === 'nevertime:menu' && event.sourceEntity?.typeId === 'minecraft:player') {
    system.run(() => openMenu(event.sourceEntity));
  }
});
world.afterEvents.itemUse.subscribe(event => {
  if (event.itemStack?.typeId === 'minecraft:compass') system.run(() => openMenu(event.source));
});
system.runInterval(() => {
  for (const player of world.getPlayers()) {
    try {
      const text = hudText(player, preferences(player), world.getTimeOfDay());
      if (text) { player.onScreenDisplay.setActionBar(text); activeBars.add(player.id); }
      else if (activeBars.delete(player.id)) player.onScreenDisplay.setActionBar('');
    } catch { /* A player can leave or change dimension between reads. Retry next interval. */ }
  }
}, 10);
world.afterEvents.playerLeave.subscribe(event => {
  cache.delete(event.playerId); openForms.delete(event.playerId); activeBars.delete(event.playerId);
});
