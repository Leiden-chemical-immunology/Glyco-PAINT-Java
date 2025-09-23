package paint.shared.utils;

public class JarInfo {
    public final String implementationTitle;
    public final String implementationVersion;
    public final String implementationVendor;
    public final String implementationDate;

    public final String specificationTitle;
    public final String specificationVersion;
    public final String specificationVendor;

    public JarInfo(String implTitle, String implVersion, String implVendor, String implDate,
                   String specTitle, String specVersion, String specVendor) {
        this.implementationTitle = implTitle;
        this.implementationVersion = implVersion;
        this.implementationVendor = implVendor;
        this.implementationDate = implDate;

        this.specificationTitle = specTitle;
        this.specificationVersion = specVersion;
        this.specificationVendor = specVendor;
    }

    @Override
    public String toString() {
        return String.format(
                "Implementation Title  : %s%n" +
                        "Implementation Version: %s%n" +
                        "Implementation Vendor : %s%n" +
                        "Implementation Date   : %s%n" +
                implementationTitle, implementationVersion, implementationVendor, implementationDate
        );
    }

    public String toStringLONG() {
        return String.format(
                "Implementation Title  : %s%n" +
                        "Implementation Version: %s%n" +
                        "Implementation Vendor : %s%n" +
                        "Implementation Date   : %s%n" +
                        "Specification Title   : %s%n" +
                        "Specification Version : %s%n" +
                        "Specification Vendor  : %s",
                implementationTitle, implementationVersion, implementationVendor,
                implementationDate, specificationTitle, specificationVersion, specificationVendor
        );
    }
}