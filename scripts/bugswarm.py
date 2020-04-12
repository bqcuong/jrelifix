import os
import subprocess

import log
from shell_wrapper import ShellWrapper

DOCKER_HUB_REPO="bugswarm/images"
SCRIPT_DEFAULT = '/bin/bash'
RUN_VALIDATION = '\"/usr/local/bin/run_failed.sh\"'

HOST_M2 = '~/.m2'
CONTAINER_M2 = '/home/travis/.m2'

HOST_SOURCE_CODE = '~/bugswarm-code'
CONTAINER_SOURCE_CODE = '/home/travis/build/failed'


def docker_run_container(image_tag, use_m2_cache, binding_source_code, interactive):
    assert isinstance(image_tag, str) and not image_tag.isspace()
    assert isinstance(use_m2_cache, bool)
    assert isinstance(binding_source_code, bool)
    assert isinstance(interactive, bool)

    # First, try to pull the image.
    ok = docker_pull(image_tag)
    if not ok:
        return False

    # create a container name which are same with the image tag
    container_name = image_tag

    # Binding m2 repository to store & reuse cached libraries
    host_m2 = _expand_path(HOST_M2)
    container_m2 = CONTAINER_M2
    if use_m2_cache:
        if not os.path.exists(host_m2):
            log.info('Creating', host_m2, 'as the host m2 repository.')
            os.makedirs(host_m2, exist_ok=True)
        log.info('Binding host m2 repository', host_m2, 'to container directory', container_m2)

    # Binding source code folder which to run repair engine on
    host_source_code = _expand_path(HOST_SOURCE_CODE)
    container_source_code = CONTAINER_SOURCE_CODE
    if binding_source_code:
        if not os.path.exists(host_source_code):
            log.info('Creating', host_source_code, 'as the host source code folder.')
            os.makedirs(host_source_code, exist_ok=True)
        log.info('Binding host source code folder', host_source_code, 'to container directory', container_source_code)

    image_location = _image_location(image_tag)

    # Prepare the arguments for the docker run command.
    volume_args = ['-v', '{}:{}'.format(host_m2, container_m2)] if use_m2_cache else []
    name_args = ['--name', container_name]
    interactive_args = ['-i', '-t'] if interactive else []
    detach_args = ['-d']
    script_args = [SCRIPT_DEFAULT]
    tail_scripts = [image_location] + script_args

    args = ['docker', 'run'] + detach_args + volume_args + name_args + interactive_args + tail_scripts
    command = ' '.join(args)
    log.info(command)
    _, _, return_code = ShellWrapper.run_commands(command, shell=True)
    return return_code == 0


def docker_run_validation(container_name):
    assert isinstance(container_name, str)  and not container_name.isspace()
    script_args = [SCRIPT_DEFAULT, '-c', RUN_VALIDATION]
    args = ['docker', 'exec', container_name] + script_args
    command = ' '.join(args)
    _, _, return_code = ShellWrapper.run_commands(command, shell=True)
    return return_code == 0


def docker_remove(container_name):
    assert isinstance(container_name, str) and not container_name.isspace()
    command = 'docker rm -f {}'.format(container_name)
    _, _, return_code = ShellWrapper.run_commands(command, shell=True)
    return return_code == 0


def docker_pull(image_tag):
    assert image_tag
    assert isinstance(image_tag, str)

    # Exit early if the image already exists locally.
    if _image_exists_locally(image_tag):
        return True

    image_location = _image_location(image_tag)
    command = 'docker pull {}'.format(image_location)
    _, _, return_code = ShellWrapper.run_commands(command, shell=True)
    if return_code != 0:
        log.error('Could not download the image', image_location, 'from Docker Hub.')
    else:
        log.info('Downloaded the image', image_location + '.')
    return return_code == 0


# Returns True if the image already exists locally.
def _docker_image_inspect(image_tag):
    image_location = _image_location(image_tag)
    command = 'docker image inspect {}'.format(image_location)
    _, _, return_code = ShellWrapper.run_commands(command,
                                                 stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL, shell=True)
    # For a non-existent image, docker image inspect has a non-zero exit status.
    if return_code == 0:
        log.info('The image', image_location, 'already exists locally and is up to date.')
    return return_code == 0


# Returns True if the image already exists locally.
def _image_exists_locally(image_tag):
    return _docker_image_inspect(image_tag)


def _image_location(image_tag):
    assert image_tag
    assert isinstance(image_tag, str)
    return DOCKER_HUB_REPO + ':' + image_tag


def _expand_path(path):
    return os.path.expanduser(path)