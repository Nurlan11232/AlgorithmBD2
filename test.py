import unittest
from main import prepare_words, greedy, dp_iterative, Word
class Test(unittest.TestCase):
    def setUp(self):
        self.mn_text = "Авто спортын хурд хүчний гайхамшиг"
        self.en_text = " i want  more sweet chocolates"
    def test_prepare_words_mn(self):
        words = prepare_words(self.mn_text, "mn_MN", use_hyphen=True)
        self.assertTrue(all(isinstance(w, Word) for w in words))
        self.assertEqual(len(words), len(self.mn_text.split()))
    def test_prepare_words_en(self):
        words = prepare_words(self.en_text, "en_US", use_hyphen=True)
        self.assertTrue(all(isinstance(w, Word) for w in words))
        self.assertEqual(len(words), len(self.en_text.split()))
    def test_greedy_output(self):
        words = prepare_words(self.en_text, "en_US", use_hyphen=True)
        lines = greedy(words, width=20)
        self.assertTrue(all(len(line) <= 20 for line in lines))
    def test_dp_output(self):
        words = prepare_words(self.en_text, "en_US", use_hyphen=True)
        lines = dp_iterative(words, width=20)
        self.assertTrue(all(len(line) <= 20 for line in lines))
if __name__ == "__main__":
    unittest.main()