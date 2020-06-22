import os
import subprocess

import log
from shell_wrapper import ShellWrapper

DOCKER_HUB_REPO="bugswarm/images"
SCRIPT_DEFAULT = '/bin/bash'
RUN_VALIDATION_ALL = '/home/travis/run_all.sh'
RUN_VALIDATION_REDUCED = '/home/travis/run_reduced.sh'
RUN_COMPILATION = '/home/travis/compile.sh'
RUN_VALIDATION_ORIGINAL = '/usr/local/bin/run_failed.sh'

ORIGINAL_BUILD_FOLDER_NAME = 'failed'
CLONED_BUILD_FOLDER_NAME = 'failed_clone'
ORIGINAL_BUILD_FOLDER_PATH = '/home/travis/build/{0}'.format(ORIGINAL_BUILD_FOLDER_NAME)

HOST_M2 = '~/.m2'
CONTAINER_M2 = '/home/travis/.m2'

HOST_SOURCE_CODE = '~/bugswarm/code'
CONTAINER_SOURCE_CODE = '/home/travis/build/{0}'.format(CLONED_BUILD_FOLDER_NAME)


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
    m2_args = ['-v', '{}:{}'.format(host_m2, container_m2)] if use_m2_cache else []
    code_args = ['-v', '{}:{}'.format(host_source_code, container_source_code)] if binding_source_code else []
    volume_args = m2_args + code_args
    name_args = ['--name', container_name]
    interactive_args = ['-i', '-t'] if interactive else []
    detach_args = ['-d']
    script_args = [SCRIPT_DEFAULT]
    tail_scripts = [image_location] + script_args

    args = ['docker', 'run'] + detach_args + volume_args + name_args + interactive_args + tail_scripts
    command = ' '.join(args)
    log.info(command)
    _, _, return_code = ShellWrapper.run_commands(command, shell=True)

    # clone the code to failed_clone to edit and run validation
    if return_code == 0 and binding_source_code:
        return _docker_clone_validation_script(container_name)

    return return_code == 0


def _docker_clone_validation_script(container_name):
    res = _docker_execute_script(container_name, 'cp {0} {1}'.format(RUN_VALIDATION_ORIGINAL, RUN_VALIDATION_ALL))
    if res:
        res = _docker_execute_script(container_name, "sed -i 's/{0}\//{1}\//g' {2}".format(ORIGINAL_BUILD_FOLDER_NAME, CLONED_BUILD_FOLDER_NAME, RUN_VALIDATION_ALL))
    return res


def docker_clone_code(container_name):
    res = _docker_execute_script(container_name, 'rm -rf {0}/*;cp -r {1}/* {0}'.format(CONTAINER_SOURCE_CODE, ORIGINAL_BUILD_FOLDER_PATH))
    return res


def _docker_execute_script(container_name, script):
    assert isinstance(container_name, str) and not container_name.isspace()
    script_args = [SCRIPT_DEFAULT, '-c', '\"' + script + '\"']
    args = ['docker', 'exec', container_name] + script_args
    command = ' '.join(args)
    _, _, return_code = ShellWrapper.run_commands(command, shell=True)
    return return_code == 0


def docker_compile(container_name):
    return _docker_execute_script(container_name, RUN_COMPILATION)


def docker_run_validation(reduced_ts, container_name):
    assert isinstance(reduced_ts, bool)
    if reduced_ts:
        res = _docker_execute_script(container_name, RUN_VALIDATION_REDUCED)
    else:
        res = _docker_execute_script(container_name, RUN_VALIDATION_ALL)
    return res


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