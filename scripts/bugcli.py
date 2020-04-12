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
def run(image_tag, codebinding, m2cache, interactive):
    """Create and run the artifact"""
    codebinding = True if codebinding else False
    m2cache = True if m2cache else False
    interactive = True if interactive else False
    res = bugswarm.docker_run_container(image_tag, codebinding, m2cache, interactive)
    sys.exit(0 if res else 1)


@cli.command(help('Validate the mutated project. Before run validation, make sure you run clone code first'))
@click.argument('container_name')
def validate(container_name):
    """Validate the mutated program. Before run validation, make sure you run clone code first"""
    res = bugswarm.docker_run_validation(container_name)
    sys.exit(0 if res else 1)


@cli.command()
@click.option('--compile-first', '-c', is_flag=True, help='Compile the original source code before cloning')
@click.argument('container_name')
def clone(compile_first, container_name):
    """Clone the original source code to new dir"""
    compile_first = True if compile_first else False
    res = bugswarm.docker_clone_code(compile_first, container_name)
    sys.exit(0 if res else 1)


@cli.command()
@click.argument('container_name')
def rm(container_name):
    """Remove the current container"""
    res = bugswarm.docker_remove(container_name)
    sys.exit(0 if res else 1)


if __name__ == '__main__':
    cli()