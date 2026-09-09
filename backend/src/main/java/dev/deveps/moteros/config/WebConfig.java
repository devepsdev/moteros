package dev.deveps.moteros.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

/** Sirve como recursos estaticos los archivos subidos a la carpeta de uploads. */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${app.uploads.dir:uploads}")
    private String uploadsDir;

    @Value("${app.uploads.public-path:/uploads}")
    private String publicPath;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String location = Paths.get(uploadsDir).toAbsolutePath().normalize().toUri().toString();
        String patron = (publicPath.endsWith("/") ? publicPath : publicPath + "/") + "**";
        registry.addResourceHandler(patron).addResourceLocations(location);
    }
}
