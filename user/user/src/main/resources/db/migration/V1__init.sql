CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE TABLE user_profiles (
                               id UUID PRIMARY KEY,

                               full_name VARCHAR(200) NOT NULL,
                               avatar_url TEXT,
                               bio TEXT,

                               city VARCHAR(100),
                               state VARCHAR(100),
                               locality VARCHAR(200),

                               budget_min DECIMAL(14,2),
                               budget_max DECIMAL(14,2),

                               looking_for VARCHAR(30),
                               property_type_pref VARCHAR[],

                               total_listings INT DEFAULT 0,
                               total_inquiries_sent INT DEFAULT 0,

                               created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                               updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                               CONSTRAINT fk_user_profiles_user
                                   FOREIGN KEY (id)
                                       REFERENCES users(id)
                                       ON DELETE CASCADE
);

CREATE TABLE agent_profiles (
                                id UUID PRIMARY KEY,

                                agency_name VARCHAR(200),

                                rera_number VARCHAR(100) UNIQUE,
                                rera_state VARCHAR(50),
                                rera_valid_till DATE,

                                experience_years INT DEFAULT 0,

                                areas_served TEXT[],
                                languages VARCHAR[],

                                kyc_doc_url TEXT,
                                kyc_verified_at TIMESTAMP,

                                avg_rating DECIMAL(3,2) DEFAULT 0,
                                total_reviews INT DEFAULT 0,
                                total_deals_closed INT DEFAULT 0,

                                subscription_tier VARCHAR(30) DEFAULT 'FREE',

                                created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                CONSTRAINT fk_agent_profiles_user
                                    FOREIGN KEY (id)
                                        REFERENCES users(id)
                                        ON DELETE CASCADE
);

CREATE TABLE builder_profiles (
                                  id UUID PRIMARY KEY,

                                  company_name VARCHAR(300) NOT NULL,
                                  company_logo_url TEXT,

                                  rera_numbers TEXT[],
                                  established_year INT,

                                  total_projects INT DEFAULT 0,
                                  total_delivered INT DEFAULT 0,

                                  website_url TEXT,

                                  avg_rating DECIMAL(3,2) DEFAULT 0,

                                  kyc_verified_at TIMESTAMP,

                                  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                  CONSTRAINT fk_builder_profiles_user
                                      FOREIGN KEY (id)
                                          REFERENCES users(id)
                                          ON DELETE CASCADE
);

CREATE TABLE saved_properties (
                                  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

                                  user_id UUID NOT NULL,
                                  listing_id UUID NOT NULL,

                                  collection_name VARCHAR(100) DEFAULT 'Saved',
                                  notes TEXT,

                                  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                  CONSTRAINT fk_saved_properties_user
                                      FOREIGN KEY (user_id)
                                          REFERENCES users(id)
                                          ON DELETE CASCADE
);

CREATE TABLE search_history (
                                id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

                                user_id UUID NOT NULL,

                                query TEXT NOT NULL,
                                filters JSONB,

                                result_count INT,
                                location VARCHAR(200),

                                created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                CONSTRAINT fk_search_history_user
                                    FOREIGN KEY (user_id)
                                        REFERENCES users(id)
                                        ON DELETE CASCADE
);

-- user profiles
CREATE INDEX idx_user_profiles_city ON user_profiles(city);
CREATE INDEX idx_user_profiles_state ON user_profiles(state);

-- agent profiles
CREATE INDEX idx_agent_profiles_rating ON agent_profiles(avg_rating);
CREATE INDEX idx_agent_profiles_subscription ON agent_profiles(subscription_tier);

-- builder profiles
CREATE INDEX idx_builder_profiles_rating ON builder_profiles(avg_rating);

-- saved properties
CREATE INDEX idx_saved_properties_user ON saved_properties(user_id);
CREATE INDEX idx_saved_properties_listing ON saved_properties(listing_id);

-- search history
CREATE INDEX idx_search_history_user ON search_history(user_id);
CREATE INDEX idx_search_history_created ON search_history(created_at);

CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
   NEW.updated_at = CURRENT_TIMESTAMP;
RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_user_profiles_updated_at
    BEFORE UPDATE ON user_profiles
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();