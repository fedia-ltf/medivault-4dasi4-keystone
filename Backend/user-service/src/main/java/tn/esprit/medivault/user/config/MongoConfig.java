package tn.esprit.medivault.user.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.EnableMongoAuditing;

/** Remplit automatiquement dateCreation (@CreatedDate). */
@Configuration
@EnableMongoAuditing
public class MongoConfig {
}
