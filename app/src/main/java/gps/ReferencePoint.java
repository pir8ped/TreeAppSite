package gps;

/**
 * Represents a GPS calibration reference point with known coordinates.
 */
public class ReferencePoint {
    private String name;
    private double latitude;
    private double longitude;

    public ReferencePoint(String name, double latitude, double longitude) {
        this.name = name;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    /**
     * Returns coordinates in Google Maps format for easy copy/paste.
     * Example: "52.520008, 13.404954"
     */
    public String getCoordinatesString() {
        return String.format("%.6f, %.6f", latitude, longitude);
    }

    /**
     * Parses a Google Maps format coordinate string.
     * 
     * @param coordString Format: "52.520008, 13.404954"
     * @return double array [latitude, longitude], or null if parsing fails
     */
    public static double[] parseCoordinates(String coordString) {
        if (coordString == null || coordString.isEmpty()) {
            return null;
        }
        try {
            String[] parts = coordString.split(",");
            if (parts.length != 2) {
                return null;
            }
            double lat = Double.parseDouble(parts[0].trim());
            double lon = Double.parseDouble(parts[1].trim());
            return new double[] { lat, lon };
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
