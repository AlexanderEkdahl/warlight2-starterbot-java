puts ARGF.read.gsub(/Output from your bot: \"(.)*\"\n\n/, '').gsub(/Round \d+\n\n/, '')
