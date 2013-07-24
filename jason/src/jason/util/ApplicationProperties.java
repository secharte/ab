package jason.util;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Properties;

public final class ApplicationProperties {
	private ApplicationProperties(){
		//constructor
	}
	// Constantes
	public final static String ARCHIVO_PROPIEDADES = "/tesina/Jason/Prioridades.properties";
 

	public final static String VALUE_TRUE = "true";

	// Propiedades
	private static Properties properties = null;
	
	
	// Métodos publicos de carga
	
	public static void loadProperties() throws IOException {
		loadProperties(ARCHIVO_PROPIEDADES);
	}
	
	public static void loadProperties(String propertiesFile) throws IOException {
		properties = new Properties();
		FileInputStream in = new FileInputStream(propertiesFile);
		properties.load(in);
		in.close();
		Enumeration props = properties.propertyNames();
		while (props.hasMoreElements()) {
			String key = (String)props.nextElement();
		}
	}
	
	// Métodos públicos para obtener propiedades

	public static String getProperty(String key) {
		try {
			if (properties == null) {
				loadProperties();
			}
		} catch (Exception e) {


		}
		String value = properties.getProperty(key);
		
		return value;
	}
	
	public static boolean getBooleanProperty(String key) {
		String value = getProperty(key);
		return value.equalsIgnoreCase(VALUE_TRUE);
	}

}
