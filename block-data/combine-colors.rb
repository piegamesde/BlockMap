#!/usr/bin/ruby

def parse_num( str )
  case str
  when /^\d+$/          ; str.to_i
  when /0x([A-Z0-9]+)$/ ; $1.to_i(16)
  else                  ; raise "Invalid number: '#{str}'"
  end
end

USAGE = <<EOS
Usage: combine-colors [<options>] <file1> <file2> ...
Options:
  -name-file <file>   ; default comments to block names loaded from this file.
                      ; Lines should be of the form <number> <tab> <name>.

Combine multiple block color maps into one, with later files
overriding color/comment values from earlier ones.
EOS

name_file = nil
files = []
args = $*.clone
while arg = args.shift
  case arg
  when '-name-file'
    name_file = args.shift
  when /^[^-]/
    files << arg
  else
    STDERR.puts "Unrecognised option: '#{arg}'"
    STDERR.puts USAGE
    exit 1
  end
end

block_colors = {}
block_comments = {}

if name_file
  open( name_file, 'r' ) do |s|
    while line = s.gets
      line.strip!
      next if line =~ /^\s*(#.*)?$/
      parts = line.split(/\t/)
      block_id = parse_num(parts[0])
      block_comments[block_id] = "# #{parts[1]}"
    end
  end
end

for f in files
  open( f, 'r' ) do |s|
    while line = s.gets
      line.strip!
      next if line =~ /^\s*(#.*)?$/
      parts = line.split(/\t/,3)
      block_id = parts[0] == 'default' ? -1 : parse_num(parts[0])
      block_colors[block_id] = parts[1] if parts[1]
      block_comments[block_id] = parts[2] if parts[2]
    end
  end
end

block_ids = block_colors.keys.sort
for block_id in block_ids
  puts "%s\t0x%08X%s" % [block_id == -1 ? 'default' : "0x%04X"%block_id, block_colors[block_id], (c = block_comments[block_id]) ? "\t"+c : '']
end
