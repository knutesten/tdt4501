package no.ntnu.falldetection.activities;


public class Vector3D {
    protected double x;
    protected double y;
    protected double z;
 
    public Vector3D() {
        x = y = z = 0;
    }
 
    public Vector3D(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }
 
    public Vector3D rotateX(double angle) {
        double rad, cosa, sina, yn, zn;
 
        rad = angle * Math.PI / 180;
        cosa = Math.cos(rad);
        sina = Math.sin(rad);
        yn = this.y * cosa - this.z * sina;
        zn = this.y * sina + this.z * cosa;
 
        return new Vector3D(this.x, yn, zn);
    }
 
    public Vector3D rotateY(double angle) {
        double rad, cosa, sina, xn, zn;
 
        rad = angle * Math.PI / 180;
        cosa = Math.cos(rad);
        sina = Math.sin(rad);
        zn = this.z * cosa - this.x * sina;
        xn = this.z * sina + this.x * cosa;
 
        return new Vector3D(xn, this.y, zn);
    }
 
    public Vector3D rotateZ(double angle) {
        double rad, cosa, sina, xn, yn;
 
        rad = angle * Math.PI / 180;
        cosa = Math.cos(rad);
        sina = Math.sin(rad);
        xn = this.x * cosa - this.y * sina;
        yn = this.x * sina + this.y * cosa;
 
        return new Vector3D(xn, yn, this.z);
    }
 
    public Vector3D project(int viewWidth, int viewHeight, float fov, float viewDistance) {
        double factor, xn, yn;
 
        factor = fov / (viewDistance + this.z);
        xn = this.x * factor + viewWidth / 2;
        yn = this.y * factor + viewHeight / 2;
 
        return new Vector3D(xn, yn, this.z);
    }
}