# rubocop:disable Metrics/BlockLength

require_relative 'boot'

require 'active_model/railtie'
require 'action_controller/railtie'
require 'action_view/railtie'
require 'sprockets/railtie'

Bundler.require(*Rails.groups)

require_relative '../app/controllers/application_controller'
require_relative '../app/history'

module Example
  class Application < Rails::Application
    # TODO: move to the Convifgutaion.
    # It needs to be moved to the Configuration, but it can't be done at the
    # moment because replacing it causes the application to crash.
    Rails.configuration.header = 'Authorization'

    config.middleware.insert_before 0, Rack::Cors do
      allow do
        origins '*'
        resource '*', headers: :any, methods: %i[get post patch delete put options]
      end
    end

    config.action_controller.perform_caching = true
    config.active_support.deprecation = :log
    config.assets.debug = false
    config.assets.digest = true
    config.cache_classes = true
    config.consider_all_requests_local = true
    config.eager_load = true
    config.require_master_key = false
    config.secret_key_base = SecureRandom.uuid

    routes.append do
      root to: 'home#index'
      match '/convert', to: 'home#convert', via: 'post'
      match '/csv', to: 'home#csv', via: 'get'
      match '/download', to: 'home#download', via: 'get'
      match '/downloadhistory', to: 'home#downloadhistory', via: 'get'
      match '/editor', to: 'home#editor', via: 'get'
      match '/files', to: 'home#files', via: 'get'
      match '/index', to: 'home#index', via: 'get'
      match '/reference', to: 'home#reference', via: 'post'
      match '/remove', to: 'home#remove', via: 'get'
      match '/rename', to: 'home#rename', via: 'post'
      match '/restore', to: 'home#restore', via: 'put'
      match '/sample', to: 'home#sample', via: 'get'
      match '/saveas', to: 'home#saveas', via: 'post'
      match '/track', to: 'home#track', via: 'post'
      match '/upload', to: 'home#upload', via: 'post'

      get(
        '/history/:file_basename',
        to: HistoryController.action('history'),
        format: false,
        defaults: {
          format: 'html'
        },
        constraints: {
          file_basename: /[^\/]*/
        }
      )
      get(
        '/history/:file_basename/:version/data',
        to: HistoryController.action('history_data'),
        format: false,
        defaults: {
          format: 'html'
        },
        constraints: {
          file_basename: /[^\/]*/
        }
      )
    end
  end
end
