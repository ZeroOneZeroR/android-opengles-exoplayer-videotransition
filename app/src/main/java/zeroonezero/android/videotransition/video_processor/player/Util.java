package zeroonezero.android.videotransition.video_processor.player;

public class Util {
    public static int[] getScaleAspectFit(int widthIn, int heightIn, int widthOut, int heightOut) {
        float aspectRatioIn = (float) widthIn / (float) heightIn;
        float aspectRatioOut = (float) widthOut / (float) heightOut;

        int w,h;
        if(aspectRatioIn < aspectRatioOut){
            h = heightOut;
            w = Math.round(h * aspectRatioIn);
        }else{
            w = widthOut;
            h = Math.round(w * 1f/aspectRatioIn);
        }

        return new int[]{w, h};
    }

    public static float[] getScaleAspectFit(int angle, int widthIn, int heightIn, int widthOut, int heightOut) {
        final float[] scale = {1, 1};
        scale[0] = scale[1] = 1;
        if (angle == 90 || angle == 270) {
            int cx = widthIn;
            widthIn = heightIn;
            heightIn = cx;
        }

        float aspectRatioIn = (float) widthIn / (float) heightIn;
        float heightOutCalculated = (float) widthOut / aspectRatioIn;

        if (heightOutCalculated < heightOut) {
            scale[1] = heightOutCalculated / heightOut;
        } else {
            scale[0] = heightOut * aspectRatioIn / widthOut;
        }

        return scale;
    }

    public static float[] getScaleAspectCrop(int angle, int widthIn, int heightIn, int widthOut, int heightOut) {
        final float[] scale = {1, 1};
        scale[0] = scale[1] = 1;
        if (angle == 90 || angle == 270) {
            int cx = widthIn;
            widthIn = heightIn;
            heightIn = cx;
        }

        float aspectRatioIn = (float) widthIn / (float) heightIn;
        float aspectRatioOut = (float) widthOut / (float) heightOut;

        if (aspectRatioIn > aspectRatioOut) {
            float widthOutCalculated = (float) heightOut * aspectRatioIn;
            scale[0] = widthOutCalculated / widthOut;
        } else {
            float heightOutCalculated = (float) widthOut / aspectRatioIn;
            scale[1] = heightOutCalculated / heightOut;
        }

        return scale;
    }
}
