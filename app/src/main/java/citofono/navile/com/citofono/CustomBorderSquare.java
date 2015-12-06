package citofono.navile.com.citofono;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.view.MotionEvent;
import android.view.View;

/**
 * Created by raeli on 04/12/2015.
 */
public class CustomBorderSquare extends View {
    Paint paint ;
    Rect rect;
    int x;
    int y;
    int width;
    int height;
    public CustomBorderSquare(Context context, int x, int y, int width, int height) {
        super(context);
        paint = new Paint();
        this.y = x;
        this.x = y;
        this.width = width;
        this.height = height;
        rect = new Rect(x, y, width, height);
    }

    @Override
    public void onDraw(Canvas canvas){
        //super.onDraw(canvas);
        // fill
        // paint.setStyle(Paint.Style.FILL);
        // paint.setColor(Color.MAGENTA);
        // canvas.drawRect(r, paint);

        // border
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(Color.YELLOW);
        paint.setStrokeWidth(50);

        canvas.drawRect(rect, paint);
        }

}
