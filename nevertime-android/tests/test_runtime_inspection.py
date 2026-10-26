"""Tests archive evidence collection, NOT a simulated Minecraft bridge."""
import io
from pathlib import Path
import sys
import tempfile
import unittest
import zipfile
sys.path.insert(0, str(Path(__file__).resolve().parents[1] / 'tools'))
from inspect_runtime import elf_machine, inspect


def archive(items):
    out = io.BytesIO()
    with zipfile.ZipFile(out, 'w') as z:
        for name, value in items.items():
            z.writestr(name, value)
    return out.getvalue()


class InspectionTest(unittest.TestCase):
    def test_elf_architectures_and_endianness(self):
        for machine, name in [(40, 'ARM'), (183, 'AArch64'), (3, 'x86'), (62, 'x86_64')]:
            for endian in ['little', 'big']:
                header = b'\x7fELF' + bytes([2, 1 if endian == 'little' else 2]) + bytes(12) + machine.to_bytes(2, endian)
                self.assertEqual(elf_machine(header), name)

    def test_invalid_elf(self):
        for data in [b'', b'\x7fELF', bytes(20), b'\x7fELF\x03\x01' + bytes(14)]:
            self.assertIsNone(elf_machine(data))

    def test_split_inventory_not_runtime_claim(self):
        apk = archive({'AndroidManifest.xml': b'fixture', 'lib/arm64-v8a/libexample.so': b'not ELF',
                       'assets/licenses/example-NOTICE.txt': b'fixture'})
        with tempfile.TemporaryDirectory() as folder:
            path = Path(folder) / 'splits.zip'
            path.write_bytes(archive({'base.apk': apk, '../ignored.txt': b'never extracted'}))
            result = inspect(path)
            self.assertEqual(len(result['apks']), 1)
            self.assertIsNone(result['apks'][0]['libraries'][0]['elf_machine'])
            self.assertIn('Does not verify', result['limitations'])
            self.assertEqual(len(result['sha256']), 64)
            self.assertEqual(list(Path(folder).iterdir()), [path])

    def test_apk_and_missing_apk(self):
        with tempfile.TemporaryDirectory() as folder:
            path = Path(folder) / 'test.apk'
            path.write_bytes(archive({'AndroidManifest.xml': b'fixture'}))
            self.assertEqual(inspect(path)['apks'][0]['libraries'], [])
            path.write_bytes(archive({'readme': b'not an apk'}))
            with self.assertRaises(ValueError):
                inspect(path)


if __name__ == '__main__':
    unittest.main()
