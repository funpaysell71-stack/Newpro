package dev.nevertime.client;
import android.content.*;
import android.content.res.ColorStateList;
import android.graphics.*;
import android.graphics.drawable.*;
import android.view.*;
import android.widget.*;

/** Original vector-driven design system; no external graphics or emoji icons. */
public final class Design {
    public static final int BG=0xFF090E18, TEXT=0xFFE6F3FA, MUTED=0xFF91A0B7, CYAN=0xFF78DEEF, PURPLE=0xFFAB9AF5;
    public static int dp(Context c,float n){return Math.round(n*c.getResources().getDisplayMetrics().density);}
    public static LinearLayout column(Context c){LinearLayout l=new LinearLayout(c);l.setOrientation(LinearLayout.VERTICAL);return l;}
    public static GradientDrawable surface(Context c,int alpha) {
        GradientDrawable d=new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{Color.argb(alpha,29,43,60),Color.argb(alpha,19,23,39)});
        d.setCornerRadius(dp(c,16));d.setStroke(dp(c,1),0xFF354259);return d;
    }
    public static LinearLayout card(LinearLayout parent){
        Context c=parent.getContext();LinearLayout v=column(c);v.setPadding(dp(c,18),dp(c,16),dp(c,18),dp(c,12));
        v.setBackground(surface(c,new SettingsStore(c).panelOpacity()*255/100));
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.bottomMargin=dp(c,12);parent.addView(v,lp);return v;
    }
    public static TextView text(LinearLayout parent,String value,int size,int color){
        Context c=parent.getContext();TextView t=new TextView(c);t.setText(value);t.setTextColor(color);t.setTextSize(size);
        t.setLineSpacing(dp(c,2),1);if(size>=18)t.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.bottomMargin=dp(c,7);parent.addView(t,p);return t;
    }
    public static Button button(LinearLayout parent,String label,String icon,boolean primary,Runnable click){
        Context c=parent.getContext();Button b=new Button(c);b.setText(label);b.setAllCaps(false);b.setTextSize(13);b.setGravity(Gravity.CENTER_VERTICAL);
        b.setTextColor(primary?0xFF09202B:TEXT);b.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));b.setStateListAnimator(null);b.setMinHeight(dp(c,48));b.setMinimumWidth(0);
        GradientDrawable background=surface(c,255);if(primary){background.setColors(new int[]{0xFF8DECF0,0xFF70A9EF});background.setStroke(0,CYAN);}
        b.setBackground(new RippleDrawable(ColorStateList.valueOf(0x336EDDF5),background,null));b.setPadding(dp(c,14),dp(c,8),dp(c,14),dp(c,8));
        Icon i=new Icon(c,icon,primary?0xFF123343:CYAN);i.setBounds(0,0,dp(c,20),dp(c,20));b.setCompoundDrawables(i,null,null,null);b.setCompoundDrawablePadding(dp(c,10));
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.topMargin=dp(c,5);lp.bottomMargin=dp(c,5);parent.addView(b,lp);
        b.setOnClickListener(v->click.run());return b;
    }
    public interface Bool {void set(boolean v);}
    public interface Number {void set(int v);}
    public static Switch toggle(LinearLayout parent,String title,boolean checked,Bool change){
        Switch v=new Switch(parent.getContext());v.setText(title);v.setTextColor(TEXT);v.setTextSize(14);v.setMinHeight(dp(v.getContext(),48));v.setChecked(checked);v.setSwitchPadding(dp(v.getContext(),12));
        v.setThumbTintList(new ColorStateList(new int[][]{new int[]{android.R.attr.state_checked},new int[]{}},new int[]{CYAN,0xFF657185}));
        parent.addView(v,new LinearLayout.LayoutParams(-1,-2));v.setOnCheckedChangeListener((b,on)->change.set(on));return v;
    }
    public static void slider(LinearLayout parent,String label,int value,int min,int max,Number changed){
        TextView t=text(parent,String.format(java.util.Locale.getDefault(),"%s  %d",label,value),12,MUTED);
        SeekBar s=new SeekBar(parent.getContext());s.setContentDescription(label);s.setMax(max-min);s.setProgress(value-min);
        parent.addView(s,new LinearLayout.LayoutParams(-1,dp(parent.getContext(),42)));
        s.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
            public void onProgressChanged(SeekBar b,int n,boolean user){t.setText(String.format(java.util.Locale.getDefault(),"%s  %d",label,n+min));}
            public void onStartTrackingTouch(SeekBar b){}
            public void onStopTrackingTouch(SeekBar b){changed.set(b.getProgress()+min);}
        });
    }
    public static void unavailable(LinearLayout parent,String title,String detail){
        LinearLayout card=card(parent);text(card,title,18,TEXT);text(card,"UNSUPPORTED • нет игрового адаптера",10,PURPLE);text(card,detail,12,MUTED);
    }
    public static final class Art extends View {
        private final Paint p=new Paint(3);
        private Shader glow; private final Drawable logo;
        public Art(Context c){super(c);logo=c.getDrawable(R.drawable.ic_nevertime);setContentDescription("Nevertime — объёмный логотип N");}
        @Override protected void onSizeChanged(int w,int h,int oldw,int oldh){glow=new RadialGradient(w*.5f,h*.5f,Math.max(1,Math.max(w,h)*.6f),new int[]{0xFF173C50,0x0010263C},null,Shader.TileMode.CLAMP);}
        @Override protected void onDraw(Canvas c){
            float w=getWidth(),h=getHeight();p.setShader(glow);c.drawRect(0,0,w,h,p);p.setShader(null);
            p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(dp(getContext(),1));p.setColor(0x33547B99);
            for(int n=0;n<3;n++)c.drawCircle(w/2,h/2,Math.min(w,h)*(.25f+n*.1f),p);
            p.setStyle(Paint.Style.FILL);int s=(int)(Math.min(w,h)*.68f);logo.setBounds((int)(w-s)/2,(int)(h-s)/2,(int)(w+s)/2,(int)(h+s)/2);logo.draw(c);
        }
    }
    public static final class Icon extends Drawable {
        private final Paint p=new Paint(3);private final String kind;
        public Icon(Context context,String kind,int color){this.kind=kind;p.setColor(color);p.setStrokeWidth(1.7f);p.setStyle(Paint.Style.STROKE);p.setStrokeCap(Paint.Cap.ROUND);}
        @Override public void draw(Canvas canvas){int save=canvas.save();canvas.translate(getBounds().left,getBounds().top);canvas.scale(getBounds().width()/24f,getBounds().height()/24f);
            switch(kind){
                case "play":Path q=new Path();q.moveTo(7,4);q.lineTo(20,12);q.lineTo(7,20);q.close();canvas.drawPath(q,p);break;
                case "modules":for(int x:new int[]{4,14})for(int y:new int[]{4,14})canvas.drawRoundRect(x,y,x+6,y+6,1,1,p);break;
                case "close":canvas.drawLine(6,6,18,18,p);canvas.drawLine(18,6,6,18,p);break;
                case "edit":canvas.drawRect(5,5,19,19,p);canvas.drawLine(12,2,12,22,p);canvas.drawLine(2,12,22,12,p);break;
                case "world":canvas.drawCircle(12,12,9,p);canvas.drawOval(8,3,16,21,p);canvas.drawLine(3,12,21,12,p);break;
                case "visual":canvas.drawOval(2,6,22,18,p);canvas.drawCircle(12,12,3,p);break;
                case "back":canvas.drawLine(16,5,8,12,p);canvas.drawLine(8,12,16,19,p);break;
                default:for(int y:new int[]{5,12,19})canvas.drawLine(3,y,21,y,p);canvas.drawCircle(8,5,2,p);canvas.drawCircle(16,12,2,p);canvas.drawCircle(10,19,2,p);
            }canvas.restoreToCount(save);
        }
        @Override public void setAlpha(int n){p.setAlpha(n);}
        @Override public void setColorFilter(ColorFilter f){p.setColorFilter(f);}
        @Override public int getOpacity(){return PixelFormat.TRANSLUCENT;}
    }
}
