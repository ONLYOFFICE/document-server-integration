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
	"context"

	"github.com/ONLYOFFICE/document-server-integration/server"
	"go.uber.org/fx"
	"go.uber.org/zap"
)

func Initialize(
	lifecycle fx.Lifecycle,
	server *server.Server,
	logger *zap.SugaredLogger,
) {
	lifecycle.Append(
		fx.Hook{
			OnStart: func(c context.Context) error {
				go server.Run()
				logger.Infof("Go server is up")
				return nil
			},
			OnStop: func(c context.Context) error {
				logger.Info("Shutting down the server")
				err := logger.Sync()
				if err != nil {
					return err
				}

				err = server.Http.Shutdown(c)
				if err != nil {
					return err
				}

				return nil
			},
		},
	)
}
