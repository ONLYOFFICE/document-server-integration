from os import environ
from sys import argv
from uuid import uuid1
from mimetypes import add_type
from django import setup
from django.conf import settings
from django.core.management import execute_from_command_line
from django.core.management.commands.runserver import Command as RunServer
from django.urls import path
from src.history import HistoryController
from src.views import actions, index

def debug():
    env = environ.get('DEBUG')
    if env is None:
        return False
    if env == 'true':
        return True
    return False

def address():
    if settings.DEBUG:
        return '127.0.0.1'
    return '0.0.0.0'

def port():
    env = environ.get('PORT')
    return env or '8000'

def configuration():
    return {
        'ALLOWED_HOSTS': [
            '*'
        ],
        'DEBUG': debug(),
        'ROOT_URLCONF': __name__,
        'SECRET_KEY': uuid1(),
        'STATIC_URL': 'static/',
        'TEMPLATES': [
            {
                'BACKEND': 'django.template.backends.django.DjangoTemplates',
                'DIRS': [
                    'templates'
                ]
            }
        ]
    }

def routers():
    history = HistoryController()
    return [
        path('', index.default),
        path('convert', actions.convert),
        path('create', actions.createNew),
        path('csv', actions.csv),
        path('download', actions.download),
        path('downloadhistory', actions.downloadhistory),
        path('edit', actions.edit),
        path('files', actions.files),
        path('reference', actions.reference),
        path('remove', actions.remove),
        path('rename', actions.rename),
        path('saveas', actions.saveAs),
        path('track', actions.track),
        path('upload', actions.upload),
        path('history/<source_basename>', history.history),
        path('history/<source_basename>/<version>/data', history.data)
    ]

add_type('text/javascript', '.js', True)
settings.configure(**configuration())
urlpatterns = routers()
RunServer.default_addr = address()
RunServer.default_port = port()
setup()

if __name__ == '__main__':
    execute_from_command_line(argv)
