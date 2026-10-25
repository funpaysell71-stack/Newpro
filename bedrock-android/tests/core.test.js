import { test } from 'node:test';
import assert from 'node:assert/strict';
import { defaults, decodePrefs, worldClock, direction, hudText } from '../behavior_pack/scripts/core.js';
test('persistent preferences validate booleans and reject corrupt input', () => {
  for (const invalid of [undefined, '', '{', 'null', '42']) assert.deepEqual(decodePrefs(invalid), defaults());
  assert.deepEqual(decodePrefs('{"enabled":false,"speed":true,"coords":"false","unknown":1}'), {...defaults(), enabled:false,speed:true});
});
test('world clock converts tick zero to 06:00, wraps midnight', () => {
  assert.equal(worldClock(0),'06:00'); assert.equal(worldClock(6000),'12:00'); assert.equal(worldClock(18000),'00:00'); assert.equal(worldClock(24000),'06:00');
});
test('Minecraft yaw compass directions wrap correctly', () => {
  assert.equal(direction(0),'S'); assert.equal(direction(90),'W'); assert.equal(direction(-90),'E'); assert.equal(direction(180),'N'); assert.equal(direction(360),'S');
});
test('HUD floors negative coordinates and labels velocity by tick', () => {
  const player={location:{x:-.1,y:64.9,z:10},dimension:{id:'minecraft:overworld'},getRotation:()=>({y:0}),getVelocity:()=>({x:.3,y:20,z:.4})};
  const text=hudText(player,{...defaults(),speed:true,clock:true,dimension:true},0);
  assert.match(text,/-1, 64, 10/); assert.match(text,/0.500 b\/tick/); assert.match(text,/06:00/); assert.match(text,/overworld/);
  assert.equal(hudText(player,{...defaults(),enabled:false},0),'');
  assert.equal(hudText(player,{enabled:true,coords:false,clock:false,speed:false,direction:false,dimension:false},0),'');
});
