package classes;

import def.Utility;

public class ParsedSize {
    private double size;
    private String unit;
    public ParsedSize(String unit, double size){
        setUnit(unit);
        setSize(size);
    }
    public String getUnit() {
        return unit;
    }
    public double getSize() {
        return size;
    }

    public void setSize(double size) {
        this.size = size;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }
    public static ParsedSize parse(double size){
        if(size > Utility.GB_SIZE){
            return new ParsedSize("GB", size/Utility.GB_SIZE);
        }else if(size > Utility.MB_SIZE){
            return new ParsedSize("MB", size/Utility.MB_SIZE);
        }else if(size > Utility.KB_SIZE){
            return new ParsedSize("KB", size/Utility.KB_SIZE);
        }else{
            return new ParsedSize("Bytes", size);
        }
    }
    public static ParsedSize parseWithTime(double size, double time){
        return parse(size/time);
    }
}
