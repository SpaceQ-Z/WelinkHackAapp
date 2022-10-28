package com.welink.hackapp.colorblobdetect;

import android.graphics.Bitmap;
import java.util.LinkedList;

/****************************
 * Copyright (C) 蔚领时代
 * 创建时间：2022/10/28 11:09
 * 项目名称：WelinkHackAapp
 * @author 赵强
 * @version 1.0
 * @since JDK 1.8.0
 * 文件名称：MyCanny
 * 类说明：
 ****************************/


public class MyCanny {
    private int Th;
    private int Tl;
    private float ratioOfTh;
    private Bitmap bitmap;
    private int h, w;
    private int[][] Gxy;
    private double[][] angle;

    private static int mayEdgePointGrayValue = 125;
    public MyCanny(Bitmap bitmap, float ratioOfTh) {
        this.bitmap = bitmap;
        this.ratioOfTh = ratioOfTh;
        init();
    }

    private void init() {
        h = bitmap.getHeight();
        w = bitmap.getWidth();
        Gxy = new int[h][w];
        angle = new double[h][w];
    }

    //得到高斯模板矩阵
    public float[][] get2DKernalData(int n, float sigma) {
        int size = 2 * n + 1;
        float sigma22 = 2 * sigma * sigma;
        float sigma22PI = (float) Math.PI * sigma22;
        float[][] kernalData = new float[size][size];
        int row = 0;
        for (int i = -n; i <= n; i++) {
            int column = 0;
            for (int j = -n; j <= n; j++) {
                float xDistance = i * i;
                float yDistance = j * j;
                kernalData[row][column] = (float) Math
                        .exp(-(xDistance + yDistance) / sigma22) / sigma22PI;
                column++;
            }
            row++;
        }

        return kernalData;
    }
    //获得图的灰度矩阵
    public int[][] getGrayMatrix(Bitmap bitmap) {
        int h = bitmap.getHeight();
        int w = bitmap.getWidth();
        int grayMatrix[][] = new int[h][w];
        for (int i = 0; i < h; i++)
            for (int j = 0; j < w; j++) {
                int argb = bitmap.getPixel(j, i);
                int r = (argb >> 16) & 0xFF;
                int g = (argb >> 8) & 0xFF;
                int b = (argb >> 0) & 0xFF;
                int grayPixel = (int) (r + g + b) / 3;
                grayMatrix[i][j] = grayPixel;
            }
        return grayMatrix;
    }

    //获得高斯模糊后的灰度矩阵
    public int[][] GS(int[][] hd, int size, float sigma) {
        float[][] gs = get2DKernalData(size, sigma);
        int outmax = 0;
        int inmax = 0;
        for (int x = size; x < w - size; x++)
            for (int y = size; y < h - size; y++) {
                float hc1 = 0;
                if (hd[y][x] > inmax)
                    inmax = hd[y][x];
                for (int k = -size; k < size + 1; k++)
                    for (int j = -size; j < size + 1; j++) {
                        hc1 = gs[size + k][j + size] * hd[y + j][x + k] + hc1;

                    }
                hd[y][x] = (int) (hc1);
                if (outmax < hc1)
                    outmax = (int) (hc1);
            }
        float rate = inmax / outmax;

        for (int x = size; x < w - size; x++)
            for (int y = size; y < h - size; y++) {
                hd[y][x] = (int) (hd[y][x] * rate);
            }
        return hd;
    }
    //获得Gxy 和angle即梯度振幅和梯度方向
    public void getGxyAndAngle(int[][] Gs) {

        for (int x = 1; x < h - 1; x++)
            for (int y = 1; y < w - 1; y++) {
                int Gx = (Gs[x][y + 1] - Gs[x][y] + Gs[x + 1][y + 1] - Gs[x + 1][y]) / 2;//hd[x][y+1]-hd[x][y];//
                int Gy = (Gs[x][y] - Gs[x + 1][y] + Gs[x][y + 1] - Gs[x + 1][y + 1]) / 2;//hd[x+1][y]-hd[x][y];//

                //另外一种算子
//                int Gx = (Gs[x - 1][y + 1] + 2 * Gs[x][y + 1]
//                        + Gs[x + 1][y + 1] - Gs[x - 1][y - 1] - 2
//                        * Gs[x][y - 1] - Gs[x + 1][y - 1]) / 4;
//                int Gy=(Gs[x-1][y-1]+2*Gs[x-1][y]+Gs[x-1][y+1]-Gs[x+1][y-1]-2*Gs[x+1][y]-Gs[x+1][y+1])/4;

                //G[x][y]=Math.sqrt(Math.pow(Gx, 2)+Math.pow(Gy, 2));
                Gxy[x][y] = (int) Math.sqrt(Gy * Gy + Gx * Gx);
                angle[x][y] = Math.atan2(Gy, Gx);
                //将梯度方向值转向(0,2*PI)
                if (angle[x][y] < 0) {
                    angle[x][y] = angle[x][y] + 2 * Math.PI;
                }
            }
    }

    //非极大值抑制,将极值点存到edge边缘矩阵中,极值点是可能为边缘的点
    public int[][] getMaxmaiLimitMatrix(int[][]Gxy,double[][]angle) {
        int[][] edge =new int[h][w];
        for (int x = 0; x < h - 1; x++)
            for (int y = 0; y < w - 1; y++) {
                double angle1 = angle[x][y] / (Math.PI);
                if ((angle1 > 0 && angle1 <= 0.25) | (angle1 > 1 && angle1 <= 1.25)) {

                    double dTmp1 = Gxy[x][y + 1] + Math.abs(Math.tan(angle[x][y]) * (Gxy[x - 1][y + 1] - Gxy[x][y + 1]));
                    double dTmp2 = Gxy[x][y - 1] + Math.abs(Math.tan(angle[x][y]) * (Gxy[x + 1][y - 1] - Gxy[x][y - 1]));

                    double dTmp = Gxy[x][y];
                    if (dTmp > dTmp1 && dTmp > dTmp2)
                        edge[x][y] = 255;
                }

                if ((angle1 <= 2 && angle1 > 1.75) | (angle1 <= 1 && angle1 > 0.75)) {

                    double dTmp1 = Gxy[x][y + 1] + Math.abs(Math.tan(angle[x][y])) * (Gxy[x + 1][y + 1] - Gxy[x][y + 1]);
                    double dTmp2 = Gxy[x][y - 1] + Math.abs(Math.tan(angle[x][y])) * (Gxy[x - 1][y - 1] - Gxy[x][y - 1]);

                    double dTmp = Gxy[x][y];
                    if (dTmp > dTmp1 && dTmp > dTmp2)
                        edge[x][y] = 255;
                }

                if ((angle1 > 1 / 4 && angle1 <= 0.5) | (angle1 > 5 / 4 && angle1 <= 1.5)) {

                    double dTmp1 = Gxy[x - 1][y] + Math.abs(1 / Math.tan(angle[x][y])) * (Gxy[x - 1][y + 1] - Gxy[x - 1][y]);
                    double dTmp2 = Gxy[x + 1][y] + Math.abs(1 / Math.tan(angle[x][y])) * (Gxy[x + 1][y - 1] - Gxy[x + 1][y]);

                    double dTmp = Gxy[x][y];
                    if (dTmp > dTmp1 && dTmp > dTmp2)
                        edge[x][y] = 255;
                }

                if ((angle1 > 1.5 && angle1 <= 1.75) | (angle1 > 0.5 && angle1 <= 0.75)) {

                    double dTmp1 = Gxy[x - 1][y] + Math.abs(1 / Math.tan(angle[x][y])) * (Gxy[x - 1][y - 1] - Gxy[x - 1][y]);
                    double dTmp2 = Gxy[x + 1][y] + Math.abs(1 / Math.tan(angle[x][y])) * (Gxy[x + 1][y + 1] - Gxy[x + 1][y]);

                    double dTmp = Gxy[x][y];
                    if (dTmp > dTmp1 && dTmp > dTmp2)
                        edge[x][y] = 255;

                }
            }
        return  edge;
    }

    public void ThTlLimitPoints(int [][] maxmaiLimitMatrix,int Th , int Tl)
    {
        //上面得到的为255的才可能是边缘点，下面根据高低阈值再次去掉小于Tl点，高于Th的仍然为255，定为边缘点，125的为预选点
        for(int x=1;x<h-1;x++)
            for(int y=1;y<w-1;y++)
            {
                if(maxmaiLimitMatrix[x][y]==255)
                {
                    if(Gxy[x][y]<Tl)
                        maxmaiLimitMatrix[x][y]=0;

                    if(Gxy[x][y]>Tl&&Gxy[x][y]<Th)
                        maxmaiLimitMatrix[x][y]=mayEdgePointGrayValue;
                }

            }
    }
    //获得高阈值
    private int getTh(int [][] Gxy)
    {
        //梯度振幅统计，因为通过计算振幅的最大值不超过500，因此用500的矩阵统计
        int []amplitudeStatistics=new int[500];
        for(int x=1;x<h-1;x++)
            for(int y=1;y<w-1;y++){
                amplitudeStatistics[Gxy[x][y]]++;
            }
        int pointNumber=0;
        int max=0;
        for(int i=1;i<500;i++){
            if(amplitudeStatistics[i]>0)
            {
                max=i;
            }
            pointNumber=pointNumber+amplitudeStatistics[i];
        }

        int ThNumber=(int)(ratioOfTh*pointNumber);
        int     ThCount=0; int Th=0;
        for(int i=1;i<=max;i++)
        {
            if(ThCount<ThNumber)
                ThCount=ThCount+amplitudeStatistics[i];
            else
            {
                Th=i-1;
                break;
            }
        }
        return Th;
    }

    private int getTl(int Th)
    {
        return (int)(Th*0.4);
    }

    //canny算法的边缘连接
    public void traceEdge(double maybeEdgePointGrayValue, int edge[][]){
        int [][]liantongbiaoji = new int [h][w];
        for(int i = 0 ; i < h ; i++)
            for(int j = 0 ; j < w; j++) {
                if(edge[i][j]==255&&liantongbiaoji[i][j]==0) {
                    if ((edge[i][j] >= maybeEdgePointGrayValue) && liantongbiaoji[i][j] == 0) {
                        liantongbiaoji[i][j] = 1;
                        LinkedList<Point> qu = new LinkedList<Point>();
                        qu.add(new Point(i, j));
                        while (!qu.isEmpty()) {
                            Point cur = qu.removeFirst();

                            for (int a = -1; a <= 1; a++)
                                for (int b = -1; b <= 1; b++) {
                                    if (cur.x + a >= 0 && cur.x + a < h && cur.y + b >= 0
                                            && cur.y + b < w) {
                                        if (edge[cur.x + a][cur.y + b] >= maybeEdgePointGrayValue
                                                && liantongbiaoji[cur.x + a][cur.y + b] == 0) {
                                            qu.add(new Point(cur.x + a, cur.y + b));
                                            liantongbiaoji[cur.x + a][cur.y + b] = 1;
                                            edge[cur.x + a][cur.y + b] = 255;
                                        }
                                    }
                                }

                        }
                    }
                }
            }
    }

    //由灰度矩阵创建灰度图
    public Bitmap createGrayImage(int[][]grayMatrix)
    {
        int h=grayMatrix.length;
        int w = grayMatrix[0].length;
        Bitmap bt=Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        for(int i=0;i<h;i++)
            for(int j=0;j<w;j++)
            {
                int grayValue=grayMatrix[i][j];
                int color = ((0xFF << 24)+(grayValue << 16)+(grayValue << 8)+grayValue);
                bt.setPixel(j, i, color);
            }
        return bt;
    }

    public Bitmap getEdgeBitmap()
    {
        int grayMatrix[][] = getGrayMatrix(bitmap);

        int GS[][] = GS(grayMatrix , 1 , 0.6f);
        getGxyAndAngle(GS);
        Th = getTh(Gxy);
        int [][] mayEdgeMatrix = getMaxmaiLimitMatrix(Gxy,angle);
        Tl = getTl(Th);
        ThTlLimitPoints(mayEdgeMatrix , Th , Tl);
        traceEdge(mayEdgePointGrayValue , mayEdgeMatrix);
        for(int x=1;x<h-1;x++)
            for(int y=1;y<w-1;y++) {
                if(mayEdgeMatrix[x][y]!=255)
                    mayEdgeMatrix[x][y]=0;
            }
        return  createGrayImage(mayEdgeMatrix);

    }

    class Point {
        Point(int a, int b) {
            this.x = a;
            this.y = b;
        }

        int x;
        int y;
    }

}
