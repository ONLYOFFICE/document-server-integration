from os import environ
from pathlib import Path
from sys import argv
from uuid import uuid1
from django import setup
from django.conf import settings
from django.core.management import execute_from_command_line
from django.core.management.commands.runserver import Command as RunServer
from django.conf.urls.static import static
from django.urls import path
from src.common import string
from src.views import actions, index

def debug():
    env = environ.get('DEBUG')
    return string.boolean(env, True)

def address():
    env = environ.get('ADDRESS')
    if env is not None:
        return env
    if settings.DEBUG:
        return RunServer.default_addr
    return '0.0.0.0'

def port():
    env = environ.get('PORT')
    if env:
        return int(env)
    return RunServer.default_port

def configuration():
    file = Path(__file__)
    base_dir = file.parent
    static_root = base_dir.joinpath('static')
    static_url = f'{static_root.name}/'
    return {
        'ALLOWED_HOSTS': [
            '*'
        ],
        'BASE_DIR': f'{base_dir}',
        'DEBUG': debug(),
        'MIDDLEWARE': [
            'src.utils.historyManager.CorsHeaderMiddleware'
        ],
        'ROOT_URLCONF': __name__,
        'SECRET_KEY': uuid1(),
        'STATIC_ROOT': f'{static_root}',
        'STATIC_URL': static_url,
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
    main = [
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
        path('restore', actions.restore),
        path('saveas', actions.saveAs),
        path('track', actions.track),
        path('upload', actions.upload)
    ]
    main += static(
        settings.STATIC_URL,
        document_root=settings.STATIC_ROOT
    )
    return main

settings.configure(**configuration())
urlpatterns = routers()
RunServer.default_addr = address()
RunServer.default_port = port()
setup()

if __name__ == '__main__':
    execute_from_command_line(argv)
