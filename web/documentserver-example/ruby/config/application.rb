require File.expand_path('../boot', __FILE__)

# Pick the frameworks you want:
require "active_model/railtie"
require "active_job/railtie"
require "active_record/railtie"
require "action_controller/railtie"
require "action_mailer/railtie"
require "action_view/railtie"
require "sprockets/railtie"
# require "rails/test_unit/railtie"

# Require the gems listed in Gemfile, including any gems
# you've limited to :test, :development, or :production.
Bundler.require(*Rails.groups)

module OnlineEditorsExampleRuby
  class Application < Rails::Application

    config.middleware.insert_before 0, Rack::Cors do
      allow do
        origins '*'
        resource '*',
                 headers: :any,
                 methods: [:get, :post, :patch, :delete, :put, :options]
      end
    end

    Rails.configuration.version="1.0.0"

    Rails.configuration.fileSizeMax=5242880
    Rails.configuration.storagePath="app_data"
    Rails.configuration.timeout=120

    Rails.configuration.viewedDocs=".pdf|.djvu|.xps|.oxps"
    Rails.configuration.editedDocs=".docx|.xlsx|.csv|.pptx|.txt"
    Rails.configuration.convertDocs=".docm|.dotx|.dotm|.dot|.doc|.odt|.fodt|.ott|.xlsm|.xltx|.xltm|.xlt|.xls|.ods|.fods|.ots|.pptm|.ppt|.ppsx|.ppsm|.pps|.potx|.potm|.pot|.odp|.fodp|.otp|.rtf|.mht|.html|.htm|.xml|.epub|.fb2"

    Rails.configuration.urlSite="https://documentserver/"
    Rails.configuration.urlConverter="ConvertService.ashx"
    Rails.configuration.urlApi="web-apps/apps/api/documents/api.js"
    Rails.configuration.urlPreloader="web-apps/apps/api/documents/cache-scripts.html"
    Rails.configuration.commandUrl="coauthoring/CommandService.ashx"

    Rails.configuration.urlExample=""

    Rails.configuration.jwtSecret = ""
    Rails.configuration.header="Authorization"

  end
end
