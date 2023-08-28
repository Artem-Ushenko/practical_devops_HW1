import unittest
import subprocess


class TestOutput(unittest.TestCase):

    def test_output(self):
        test = subprocess.run(["python3", "hello_world.py"], capture_output=True, text=True) 
        self.assertEqual(test.stdout.strip(), 'Hello, World!' )


if __name__ == '__main__':
    unittest.main()
