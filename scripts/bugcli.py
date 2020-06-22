#!/usr/bin/env python3

import logging
import sys

import click

import bugswarm
import log


@click.group()
@click.version_option(message='The BugSwarm Validation CLI')
def cli():
    log.config_logging(getattr(logging, 'DEBUG', None))
    pass


@cli.command()
@click.option('--code-binding', '-c', default=True)
@click.option('--m2cache', '-m', default=True)
@click.option('--interactive', default=True)
@click.argument('image_tag')
def run(image_tag, code_binding, m2cache, interactive):
    """Create and run the artifact"""
    code_binding = True if code_binding else False
    m2cache = True if m2cache else False
    interactive = True if interactive else False
    res = bugswarm.docker_run_container(image_tag, code_binding, m2cache, interactive)
    sys.exit(0 if res else 1)


@cli.command()
@click.option('--reduced-ts', '-r', is_flag=True, help='Only validate on the reduced test suite')
@click.argument('container_name')
def validate(reduced_ts, container_name):
    """Validate the mutated program. Before run validation, make sure you run clone code first"""
    res = bugswarm.docker_run_validation(reduced_ts, container_name)
    sys.exit(0 if res else 1)


@cli.command()
@click.argument('container_name')
def compile(container_name):
    """Compile the project"""
    res = bugswarm.docker_compile(container_name)
    sys.exit(0 if res else 1)


@cli.command()
@click.argument('container_name')
def clone(container_name):
    """Clone the original source code to new dir"""
    res = bugswarm.docker_clone_code(container_name)
    sys.exit(0 if res else 1)


@cli.command()
@click.argument('container_name')
def rm(container_name):
    """Remove the current container"""
    res = bugswarm.docker_remove(container_name)
    sys.exit(0 if res else 1)


if __name__ == '__main__':
    cli()