require 'test/minirunit'
test_check "Test return(s):"

def meth1
  (1..10).each do |val|
    return val
  end
end

test_ok(1 == meth1)

def meth2(&b)
  b
end

res = meth2 { return }

test_exception(LocalJumpError){ res.call }

def meth3
  (1..10).each do |v1|
     ('a'..'z').each do |v2|
        return v2
     end
  end
end

test_ok('a' == meth3)

def meth4
  p = Proc.new { return 99 }
  p.call
  puts "Never get here"
end

test_ok(99 == meth4)

q = Proc.new { return 99 }

def meth5(p)
  p.call
end

test_exception(LocalJumpError) { meth5(q) } 

def meth6
  p = lambda { return 99 }
  test_ok(99 == p.call)
end

meth6

class B
  attr_reader :arr
  def initialize(arr)
    @arr = arr
  end
  def detect (nothing_found = nil)
    z = each { |e| return e if yield(e) }
    # should not get here if return hit
    puts "DOH #{z}"
    nothing_found.call unless nothing_found.nil?
    nil
  end
  def each
    i = 0
    loop do
      break if i == @arr.size
      yield @arr[i]
      i+=1
    end
  end
end


test_ok(2 == B.new([1, 2, 3, 4]).detect {|c| c > 1})
