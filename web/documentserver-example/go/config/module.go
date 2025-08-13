/**
 *
 * (c) Copyright Ascensio System SIA 2025
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package config

import (
	"path/filepath"
	"runtime"
	"time"

	"github.com/spf13/viper"
	"go.uber.org/fx"
)

type ApplicationConfig struct {
	Version                  string            `mapstructure:"VERSION"`
	ServerAddress            string            `mapstructure:"SERVER_ADDRESS"`
	ServerPort               string            `mapstructure:"SERVER_PORT"`
	DocumentServerHost       string            `mapstructure:"DOC_SERVER_HOST"`
	DocumentServerConverter  string            `mapstructure:"DOC_SERVER_CONVERTER_URL"`
	DocumentServerApi        string            `mapstructure:"DOC_SERVER_API_URL"`
	DocumentServerPreloader  string            `mapstructure:"DOC_SERVER_PRELOADER_URL"`
	DocumentServerCommandUrl string            `mapstructure:"DOC_SERVER_COMMAND_URL"`
	JwtEnabled               bool              `mapstructure:"JWT_IS_ENABLED"`
	JwtExpiresIn             time.Duration     `mapstructure:"JWT_EXPIRES_IN"`
	JwtHeader                string            `mapstructure:"JWT_HEADER"`
	JwtSecret                string            `mapstructure:"JWT_SECRET"`
	StoragePath              string            `mapstructure:"STORAGE_PATH"`
	Plugins                  string            `mapstructure:"PLUGINS"`
	LoggerDebug              bool              `mapstructure:"LOGGER_DEBUG"`
	ForgottenEnabled         bool              `mapstructure:"FORGOTTEN_ENABLED"`
	Languages                map[string]string `mapstructure:"LANGUAGES"`
}

func NewConfiguration() (app_config ApplicationConfig, err error) {
	_, b, _, _ := runtime.Caller(0)
	basepath := filepath.Dir(b)

	viper.AddConfigPath(basepath)
	viper.SetConfigName("configuration")
	viper.SetConfigType("json")

	viper.AutomaticEnv()
	err = viper.ReadInConfig()

	if err != nil {
		return ApplicationConfig{}, err
	}

	err = viper.Unmarshal(&app_config)

	if err != nil {
		return ApplicationConfig{}, err
	}

	return
}

var ConfigurationModule = fx.Options(
	fx.Provide(NewConfiguration),
)
