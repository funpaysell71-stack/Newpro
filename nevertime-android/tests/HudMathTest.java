import dev.nevertime.client.HudMath;
public class HudMathTest {
    private static void check(boolean v) { if(!v)throw new AssertionError(); }
    public static void main(String[] args) {
        check(HudMath.position(.5f,1000,200)==400);
        check(HudMath.position(1f,100,200)==0);
        check(HudMath.position(Float.NaN,1000,200)==0);
        check(HudMath.normalized(400,1000,200)==.5f);
        check(HudMath.normalized(-50,1000,200)==0);
        check(HudMath.normalized(2000,1000,200)==1);
        check(HudMath.normalized(1,100,200)==0);
        check(HudMath.clamp(200,30,100)==100);
        check(HudMath.clamp(-5,10,26)==10);
        check(HudMath.duration(-1).equals("00:00:00"));
        check(HudMath.duration(3661000).equals("01:01:01"));
        System.out.println("HudMath: 11 regression assertions passed");
    }
}
