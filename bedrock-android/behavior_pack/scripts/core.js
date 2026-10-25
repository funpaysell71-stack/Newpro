export const defaults = () => ({ enabled: true, coords: true, clock: false, speed: false, direction: true, dimension: false });
export function decodePrefs(raw) {
  const result = defaults();
  try {
    const parsed = JSON.parse(raw);
    for (const key of Object.keys(result)) if (typeof parsed?.[key] === 'boolean') result[key] = parsed[key];
  } catch { /* A missing or corrupt property starts with safe defaults. */ }
  return result;
}
export function worldClock(ticks) {
  const minutes = Math.floor((((ticks % 24000) + 24000 + 6000) % 24000) / 1000 * 60);
  return `${String(Math.floor(minutes / 60)).padStart(2, '0')}:${String(minutes % 60).padStart(2, '0')}`;
}
export function direction(yaw) {
  return ['S', 'W', 'N', 'E'][Math.round(((yaw % 360) + 360) % 360 / 90) % 4];
}
export function hudText(player, prefs, ticks) {
  if (!prefs.enabled) return '';
  const parts = [];
  const { x, y, z } = player.location;
  if (prefs.coords) parts.push(`§bXYZ §f${Math.floor(x)}, ${Math.floor(y)}, ${Math.floor(z)}`);
  if (prefs.direction) parts.push(`§d${direction(player.getRotation().y)}`);
  if (prefs.dimension) parts.push(`§7${player.dimension.id.replace('minecraft:', '')}`);
  if (prefs.clock) parts.push(`§e${worldClock(ticks)}`);
  if (prefs.speed) {
    const velocity = player.getVelocity();
    // Velocity uses blocks per game tick; no assumed server tick rate / wall time.
    parts.push(`§a${Math.hypot(velocity.x, velocity.z).toFixed(3)} b/tick`);
  }
  return parts.join('  §8|  ');
}
