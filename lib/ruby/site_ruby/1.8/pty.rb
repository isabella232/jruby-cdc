require 'ffi'

module PTY
  private
  module LibUtil
    extend JRuby::FFI::Library
    # forkpty(3) is in libutil on linux, libc on MacOS/BSD
    if JRuby::FFI::Platform::IS_LINUX
      ffi_lib 'libutil'
    end
    attach_function :forkpty, [ :buffer_out, :buffer_out, :buffer_in, :buffer_in ], :pid_t
  end
  module LibC
    extend JRuby::FFI::Library
    attach_function :close, [ :int ], :int
    attach_function :strerror, [ :int ], :string
    attach_function :execv, [ :string, :buffer_in ], :int
    attach_function :execvp, [ :string, :buffer_in ], :int
    attach_function :dup2, [ :int, :int ], :int
    attach_function :dup, [ :int ], :int
  end
  Buffer = JRuby::FFI::Buffer
  def self.build_args(args)
    cmd = args.shift
    cmd_args = args.map do |arg|
      MemoryPointer.from_string(arg)
    end
    exec_args = MemoryPointer.new(:pointer, 1 + cmd_args.length + 1)
    exec_cmd = MemoryPointer.from_string(cmd)
    exec_args[0].put_pointer(0, exec_cmd)
    cmd_args.each_with_index do |arg, i|
      exec_args[i + 1].put_pointer(0, arg)
    end
    [ cmd, exec_args ]
  end
  public
  def self.getpty(*args)
    mfdp = Buffer.new :int
    name = Buffer.new 1024
    #
    # All the execv setup is done in the parent, since doing anything other than
    # execv in the child after fork is really flakey
    #
    exec_cmd, exec_args = build_args(args)
    pid = LibUtil.forkpty(mfdp, name, nil, nil)
    raise "forkpty failed: #{LibC.strerror(FFI.errno)}" if pid < 0    
    if pid == 0
      LibC.execvp(exec_cmd, exec_args)
      exit 1
    end
    masterfd = mfdp.get_int(0)
    rfp = FFI::IO.for_fd(masterfd, "r")
    wfp = FFI::IO.for_fd(LibC.dup(masterfd), "w")
    if block_given?
      yield rfp, wfp, pid
      rfp.close unless rfp.closed?
      wfp.close unless wfp.closed?
    else
      [ rfp, wfp, pid ]
    end
  end
  def self.spawn(*args, &block)
    self.getpty("/bin/sh", "-c", args[0], &block)
  end
end