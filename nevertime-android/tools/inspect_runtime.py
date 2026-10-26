"""Read-only APK/split inventory. Never loads code or infers a working game bridge."""
import argparse
import hashlib
import io
import json
from pathlib import Path
import struct
import zipfile

MAX_APK = 512 * 1024 * 1024
MAX_ENTRIES = 100000


def elf_machine(header):
    if len(header) < 20 or header[:4] != b'\x7fELF' or header[4] not in (1, 2) or header[5] not in (1, 2):
        return None
    value = struct.unpack('<H' if header[5] == 1 else '>H', header[18:20])[0]
    return {40: 'ARM', 183: 'AArch64', 3: 'x86', 62: 'x86_64'}.get(value, f'unknown:{value}')


def inspect_apk(stream, name):
    with zipfile.ZipFile(stream) as apk:
        if len(apk.infolist()) > MAX_ENTRIES:
            raise ValueError('Too many APK entries')
        libraries = []
        notices = []
        for item in apk.infolist():
            # Names are reported only: no extraction or filesystem path traversal.
            if item.filename.startswith('lib/') and item.filename.endswith('.so'):
                with apk.open(item) as library:
                    machine = elf_machine(library.read(20))
                libraries.append({'path': item.filename, 'bytes': item.file_size, 'elf_machine': machine})
            if item.filename.startswith('assets/licenses/'):
                notices.append(item.filename)
        return {'apk': name, 'has_manifest': 'AndroidManifest.xml' in apk.namelist(),
                'libraries': libraries, 'license_notice_paths': notices}


def inspect(path):
    digest = hashlib.sha256()
    with path.open('rb') as src:
        while block := src.read(1024 * 1024):
            digest.update(block)
    apks = []
    with zipfile.ZipFile(path) as archive:
        if len(archive.infolist()) > MAX_ENTRIES:
            raise ValueError('Too many archive entries')
        if 'AndroidManifest.xml' in archive.namelist():
            apks.append(inspect_apk(path, path.name))
        else:
            total = 0
            for item in archive.infolist():
                if not item.filename.endswith('.apk'):
                    continue
                total += item.file_size
                if total > MAX_APK:
                    raise ValueError('Nested APK byte budget exceeded')
                apks.append(inspect_apk(io.BytesIO(archive.read(item)), item.filename))
    if not apks:
        raise ValueError('No APK found')
    return {'file': path.name, 'sha256': digest.hexdigest(), 'apks': apks,
            'limitations': 'Inventory only. Does not verify signatures, licenses, player access, compatibility or runtime behavior.'}


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('archive', type=Path)
    args = parser.parse_args()
    try:
        print(json.dumps(inspect(args.archive), indent=2, ensure_ascii=False))
    except (OSError, ValueError, zipfile.BadZipFile, RuntimeError) as error:
        parser.exit(1, f'Inspection failed: {error}\n')
