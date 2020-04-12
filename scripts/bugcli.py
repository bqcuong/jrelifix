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
@click.option('--codebinding', '-c', default=True)
@click.option('--m2cache', '-m', default=True)
@click.option('--interactive', default=True)
@click.argument('image_tag')
def run(image_tag, codebinding, m2cache, interactive):
    res = bugswarm.docker_run_container(image_tag, codebinding, m2cache, interactive)
    sys.exit(0 if res else 1)


@cli.command()
@click.argument('container_name')
def validate(container_name):
    res = bugswarm.docker_run_validation(container_name)
    sys.exit(0 if res else 1)


@cli.command()
@click.argument('container_name')
def rm(container_name):
    res = bugswarm.docker_remove(container_name)
    sys.exit(0 if res else 1)


if __name__ == '__main__':
    cli()