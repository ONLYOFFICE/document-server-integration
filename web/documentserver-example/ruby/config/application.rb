# frozen_string_literal: true

require_relative 'boot'

require 'active_model/railtie'
require 'action_controller/railtie'
require 'action_view/railtie'
require 'sprockets/railtie'

Bundler.require(*Rails.groups)

require 'securerandom'

# Configuration for the Rails application.
class Application < Rails::Application
  config.middleware.insert_before(0, Rack::Cors) do
    allow do
      origins '*'
      resource '*', headers: :any, methods: [:get, :post, :patch, :delete, :put, :options]
    end
  end

  config.assets.debug = true
  config.assets.digest = false
  config.eager_load = false
  config.hosts << /.*/
  config.require_master_key = false
  config.secret_key_base = SecureRandom.uuid

  routes.append do
    root to: 'home#index'
    match '/convert', to: 'home#convert', via: 'post'
    match '/csv', to: 'home#csv', via: 'get'
    match '/asset', to: 'home#assets', via: 'get'
    match '/download', to: 'home#download', via: 'get'
    match '/downloadhistory', to: 'home#downloadhistory', via: 'get'
    match '/historyobj', to: 'home#historyobj', via: 'post'
    match '/editor', to: 'home#editor', via: 'get'
    match '/forgotten', to: 'home#forgotten', via: 'get'
    match '/forgotten', to: 'home#delete_forgotten', via: 'delete'
    match '/files', to: 'home#files', via: 'get'
    match '/index', to: 'home#index', via: 'get'
    match '/reference', to: 'home#reference', via: 'post'
    match '/remove', to: 'home#remove', via: 'delete'
    match '/rename', to: 'home#rename', via: 'post'
    match '/restore', to: 'home#restore', via: 'put'
    match '/sample', to: 'home#sample', via: 'get'
    match '/saveas', to: 'home#saveas', via: 'post'
    match '/track', to: 'home#track', via: 'post'
    match '/upload', to: 'home#upload', via: 'post'
    match '/formats', to: 'home#formats', via: 'get'
    match '/config', to: 'home#refresh_config', via: 'get'
  end
end
