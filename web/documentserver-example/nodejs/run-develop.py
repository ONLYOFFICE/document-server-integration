#!/usr/bin/env python
import sys
sys.path.append('../../../../build_tools/scripts')
import os
import base

def install_module():
  base.print_info('Install')
  base.cmd('npm', ['install'])

def run_integration_example():
  install_module()
  base.set_env('NODE_CONFIG_DIR', './config')
  base.print_info('run integration example')
  base.run_nodejs(['bin/www'])

base.set_env('NODE_ENV', 'development-' + base.host_platform())

run_integration_example()
